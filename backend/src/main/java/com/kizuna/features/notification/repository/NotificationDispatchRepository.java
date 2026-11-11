package com.kizuna.features.notification.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * FE-12 giai đoạn 2: đợt gửi thông báo và kết quả từng người/kênh.
 * Chốt người nhận bằng insert-select một lần trong transaction; ràng buộc UNIQUE(dispatch,user,channel)
 * và UNIQUE(notification,user) bảo đảm không ai nhận trùng khi xử lý lại.
 */
@Repository
public class NotificationDispatchRepository {
    public record NewDispatch(UUID id, String origin, UUID creatorId, UUID submissionKey, String eventKey, String type, String title, String content,
                              boolean urgent, String targetMode, List<String> roles, List<String> channels, OffsetDateTime scheduledAt,
                              Integer previewCount, String status, String actionPath) {}
    public record Dispatch(UUID id, UUID creatorId, String creatorName, String type, String title, String content, boolean urgent, String targetMode,
                           List<String> roles, List<String> channels, OffsetDateTime scheduledAt, OffsetDateTime createdAt, OffsetDateTime frozenAt,
                           Integer previewCount, Integer recipientCount, String status) {}
    public record Claimed(UUID deliveryId, UUID dispatchId, UUID userId, int attempts) {}
    public record EmailTarget(String email, boolean active, boolean emailEnabled, String title, String content) {}

    /**
     * Người nhận được phép: tài khoản còn hoạt động, vai trò còn bật; đợt theo vai trò thì thuộc vai trò đã chọn,
     * đợt từ sự kiện nghiệp vụ thì đúng những người module nguồn đã chỉ định (tham số mảng đầu tiên).
     */
    private static String eligible(boolean explicit) {
        return " FROM notification_dispatches d JOIN users u ON u.account_status::text='ACTIVE' AND u.deleted_at IS NULL"
                + (explicit ? " AND u.user_id=ANY(?::uuid[])" : "")
                + " JOIN roles r ON r.role_id=u.role_id AND r.is_active=true" + (explicit ? "" : " AND r.role_name=ANY(d.target_roles)")
                + " LEFT JOIN notification_preferences p ON p.user_id=u.user_id AND p.notification_type=d.notification_type WHERE d.dispatch_id=?";
    }
    private static final String DISPATCH = "SELECT d.dispatch_id,d.creator_id,c.full_name,d.notification_type::text,d.title,d.content,d.urgent,d.target_mode,d.target_roles,d.channels,"
            + "d.scheduled_at,d.created_at,d.frozen_at,d.preview_count,d.recipient_count,d.status FROM notification_dispatches d LEFT JOIN users c ON c.user_id=d.creator_id";

    private final JdbcTemplate jdbc;

    public NotificationDispatchRepository(@Qualifier("notificationJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static List<String> list(Array array) throws java.sql.SQLException {
        return array == null ? List.of() : List.of((String[]) array.getArray());
    }

    private static Dispatch map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Dispatch(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getBoolean(7), rs.getString(8), list(rs.getArray(9)), list(rs.getArray(10)), rs.getObject(11, OffsetDateTime.class),
                rs.getObject(12, OffsetDateTime.class), rs.getObject(13, OffsetDateTime.class), (Integer) rs.getObject(14), (Integer) rs.getObject(15), rs.getString(16));
    }

    /** Số người dự kiến nhận nếu chốt ngay bây giờ (chỉ để xem trước, không giữ chỗ). */
    public int estimate(List<String> roles) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.account_status::text='ACTIVE'"
                + " AND u.deleted_at IS NULL AND r.is_active=true AND r.role_name=ANY(?::text[])", Integer.class, (Object) roles.toArray(String[]::new)));
    }

    /** Thêm đợt mới; trùng mã thao tác của cùng người tạo (hoặc trùng mã sự kiện) thì không thêm và trả false. */
    public boolean insert(NewDispatch d) {
        return jdbc.update("INSERT INTO notification_dispatches(dispatch_id,origin,creator_id,submission_key,event_key,notification_type,title,content,urgent,target_mode,"
                + "target_roles,channels,scheduled_at,preview_count,status" + (d.actionPath() == null ? "" : ",action_path") + ") VALUES (?,?,?,?,?,?::notification_type_enum,?,?,?,?,?::text[],?::text[],?,?,?"
                + (d.actionPath() == null ? "" : ",?") + ") ON CONFLICT DO NOTHING",
                d.actionPath() == null
                        ? new Object[]{d.id(), d.origin(), d.creatorId(), d.submissionKey(), d.eventKey(), d.type(), d.title(), d.content(), d.urgent(), d.targetMode(),
                        d.roles().toArray(String[]::new), d.channels().toArray(String[]::new), d.scheduledAt(), d.previewCount(), d.status()}
                        : new Object[]{d.id(), d.origin(), d.creatorId(), d.submissionKey(), d.eventKey(), d.type(), d.title(), d.content(), d.urgent(), d.targetMode(),
                        d.roles().toArray(String[]::new), d.channels().toArray(String[]::new), d.scheduledAt(), d.previewCount(), d.status(), d.actionPath()}) == 1;
    }

    public Optional<Dispatch> bySubmission(UUID creator, UUID key) {
        return jdbc.query(DISPATCH + " WHERE d.creator_id=? AND d.submission_key=?", (rs, n) -> map(rs), creator, key).stream().findFirst();
    }

    public Optional<Dispatch> find(UUID id) {
        return jdbc.query(DISPATCH + " WHERE d.dispatch_id=?", (rs, n) -> map(rs), id).stream().findFirst();
    }

    /**
     * Chốt tập người nhận đúng một lần (khóa dòng đợt gửi; đã chốt thì bỏ qua): tạo bản trong ứng dụng cho người được nhận,
     * xếp hàng email cho người đã bật email, ghi "bỏ qua theo tùy chọn" cho kênh bị tắt. Phải gọi trong transaction.
     */
    public boolean freeze(UUID id) { return freeze(id, null); }

    /** Như trên; `explicit` khác null là đợt từ sự kiện nghiệp vụ, chỉ gửi đúng những người này (vẫn lọc tài khoản còn hoạt động và tùy chọn). */
    public boolean freeze(UUID id, Collection<UUID> explicit) {
        var rows = jdbc.query("SELECT frozen_at IS NULL FROM notification_dispatches WHERE dispatch_id=? FOR UPDATE", (rs, n) -> rs.getBoolean(1), id);
        if (rows.isEmpty() || !rows.get(0)) return false;
        var notification = UUID.randomUUID();
        boolean withPath = explicit != null;
        jdbc.update("INSERT INTO notifications(notification_id,title,content,notification_type" + (withPath ? ",action_path" : "") + ") SELECT ?,title,content,notification_type"
                + (withPath ? ",action_path" : "") + " FROM notification_dispatches WHERE dispatch_id=?", notification, id);
        Object[] eligibleArgs = explicit == null ? new Object[]{id} : new Object[]{explicit.toArray(UUID[]::new), id};
        // Khẩn toàn hệ thống: bản trong ứng dụng luôn được phát dù người nhận đã tắt (FR-006).
        jdbc.update("INSERT INTO notification_deliveries(dispatch_id,user_id,channel,state,reason) SELECT d.dispatch_id,u.user_id,'IN_APP',"
                + "CASE WHEN d.urgent OR coalesce(p.in_app_enabled,true) THEN 'IN_APP_AVAILABLE' ELSE 'SKIPPED' END,"
                + "CASE WHEN d.urgent OR coalesce(p.in_app_enabled,true) THEN NULL ELSE 'PREFERENCE' END" + eligible(explicit != null) + " AND 'IN_APP'=ANY(d.channels) ON CONFLICT DO NOTHING", eligibleArgs);
        // Email không có quyền bỏ qua tùy chọn, kể cả thông báo khẩn.
        jdbc.update("INSERT INTO notification_deliveries(dispatch_id,user_id,channel,state,reason,next_attempt_at) SELECT d.dispatch_id,u.user_id,'EMAIL',"
                + "CASE WHEN coalesce(p.email_enabled,false) AND nullif(btrim(u.email),'') IS NOT NULL THEN 'PENDING' ELSE 'SKIPPED' END,"
                + "CASE WHEN NOT coalesce(p.email_enabled,false) THEN 'PREFERENCE' WHEN nullif(btrim(u.email),'') IS NULL THEN 'NO_EMAIL' END,now()"
                + eligible(explicit != null) + " AND 'EMAIL'=ANY(d.channels) ON CONFLICT DO NOTHING", eligibleArgs);
        jdbc.update("INSERT INTO user_notifications(notification_id,user_id,received_at) SELECT ?,user_id,now() FROM notification_deliveries"
                + " WHERE dispatch_id=? AND channel='IN_APP' AND state='IN_APP_AVAILABLE' ON CONFLICT DO NOTHING", notification, id);
        jdbc.update("UPDATE notification_dispatches SET frozen_at=now(),notification_id=?,recipient_count=(SELECT count(DISTINCT user_id) FROM notification_deliveries WHERE dispatch_id=?)"
                + " WHERE dispatch_id=?", notification, id, id);
        refreshStatus(id);
        return true;
    }

    /** Trạng thái tổng hợp tính từ kết quả từng lượt (FR-012); chưa chốt thì giữ nguyên. */
    public void refreshStatus(UUID id) {
        jdbc.update("UPDATE notification_dispatches d SET status=CASE WHEN d.recipient_count=0 THEN 'NO_RECIPIENTS' WHEN s.pending>0 THEN 'SENDING'"
                + " WHEN s.unknown>0 THEN 'NEEDS_REVIEW' WHEN s.failed>0 AND s.ok>0 THEN 'PARTIAL' WHEN s.failed>0 THEN 'FAILED' ELSE 'COMPLETED' END"
                + " FROM (SELECT count(*) FILTER (WHERE state IN ('PENDING','SENDING')) pending,count(*) FILTER (WHERE state='UNKNOWN') unknown,"
                + "count(*) FILTER (WHERE state='FAILED') failed,count(*) FILTER (WHERE state IN ('IN_APP_AVAILABLE','EMAIL_ACCEPTED','SKIPPED')) ok"
                + " FROM notification_deliveries WHERE dispatch_id=?) s WHERE d.dispatch_id=? AND d.frozen_at IS NOT NULL", id, id);
    }

    /** Tài khoản đang hoạt động của một vai trò (ví dụ mọi Quản trị viên nhận sự cố mới). */
    public List<UUID> activeUsers(String role) {
        return jdbc.query("SELECT u.user_id FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active=true AND r.role_name=?",
                (rs, n) -> rs.getObject(1, UUID.class), role);
    }

    public Optional<String> roleOf(UUID user) {
        return jdbc.query("SELECT r.role_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.user_id=?", (rs, n) -> rs.getString(1), user).stream().findFirst();
    }

    public List<UUID> dueScheduled(int limit) {
        return jdbc.query("SELECT dispatch_id FROM notification_dispatches WHERE status='SCHEDULED' AND scheduled_at<=now() ORDER BY scheduled_at LIMIT ?",
                (rs, n) -> rs.getObject(1, UUID.class), limit);
    }

    /** Đang gửi mà quá 2 phút không có kết quả (máy chủ dừng giữa chừng): không biết thư đã đi chưa nên ghi "chưa xác định", không gửi lại. */
    public List<UUID> markInterrupted() {
        return jdbc.query("UPDATE notification_deliveries SET state='UNKNOWN',reason='INTERRUPTED',updated_at=now() WHERE state='SENDING' AND updated_at<now()-interval '2 minutes'"
                + " RETURNING dispatch_id", (rs, n) -> rs.getObject(1, UUID.class));
    }

    /** Giữ chỗ tối đa `limit` email đến hạn (chuyển sang SENDING trước khi gửi). */
    public List<Claimed> claimEmails(int limit) {
        return jdbc.query("UPDATE notification_deliveries SET state='SENDING',attempts=attempts+1,updated_at=now() WHERE delivery_id IN (SELECT delivery_id FROM notification_deliveries"
                + " WHERE state='PENDING' AND channel='EMAIL' AND next_attempt_at<=now() ORDER BY next_attempt_at LIMIT ? FOR UPDATE SKIP LOCKED)"
                + " RETURNING delivery_id,dispatch_id,user_id,attempts", (rs, n) -> new Claimed(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getObject(3, UUID.class), rs.getInt(4)), limit);
    }

    /** Kiểm lại người nhận ngay trước khi gửi: còn hoạt động và còn bật email cho nhóm này không. */
    public Optional<EmailTarget> emailTarget(UUID dispatch, UUID user) {
        return jdbc.query("SELECT u.email,(u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active),coalesce(p.email_enabled,false),d.title,d.content"
                + " FROM notification_dispatches d JOIN users u ON u.user_id=? JOIN roles r ON r.role_id=u.role_id"
                + " LEFT JOIN notification_preferences p ON p.user_id=u.user_id AND p.notification_type=d.notification_type WHERE d.dispatch_id=?",
                (rs, n) -> new EmailTarget(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3), rs.getString(4), rs.getString(5)), user, dispatch).stream().findFirst();
    }

    /** Ghi kết quả; chỉ ghi khi lượt vẫn đang SENDING để không đè kết quả khác. */
    public void finish(UUID delivery, String state, String reason, OffsetDateTime nextAttempt) {
        jdbc.update("UPDATE notification_deliveries SET state=?,reason=?,next_attempt_at=coalesce(?,next_attempt_at),updated_at=now() WHERE delivery_id=? AND state='SENDING'",
                state, reason, nextAttempt, delivery);
    }

    public record HistoryFilter(OffsetDateTime from, OffsetDateTime to, String status, String channel, String role, String title) {}

    private String historyWhere(HistoryFilter f, List<Object> args) {
        var sql = new StringBuilder(" WHERE d.origin='ADMIN'");
        if (f.from() != null) { sql.append(" AND d.created_at>=?"); args.add(f.from()); }
        if (f.to() != null) { sql.append(" AND d.created_at<?"); args.add(f.to()); }
        if (f.status() != null) { sql.append(" AND d.status=?"); args.add(f.status()); }
        if (f.channel() != null) { sql.append(" AND ?=ANY(d.channels)"); args.add(f.channel()); }
        if (f.role() != null) { sql.append(" AND ?=ANY(d.target_roles)"); args.add(f.role()); }
        if (f.title() != null) { sql.append(" AND d.title ILIKE ? ESCAPE '\\'"); args.add("%" + f.title().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"); }
        return sql.toString();
    }

    public List<Dispatch> history(HistoryFilter filter, int page, int size) {
        var args = new ArrayList<>();
        var sql = DISPATCH + historyWhere(filter, args) + " ORDER BY d.created_at DESC,d.dispatch_id DESC LIMIT ? OFFSET ?";
        args.add(size); args.add((long) page * size);
        return jdbc.query(sql, (rs, n) -> map(rs), args.toArray());
    }

    public long historyCount(HistoryFilter filter) {
        var args = new ArrayList<>();
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM notification_dispatches d" + historyWhere(filter, args), Long.class, args.toArray()));
    }

    /** Số lượt theo kênh và trạng thái của một đợt, đối chiếu được với kết quả từng người. */
    public Map<String, Map<String, Long>> counts(UUID id) {
        var result = new LinkedHashMap<String, Map<String, Long>>();
        jdbc.query("SELECT channel,state,count(*) FROM notification_deliveries WHERE dispatch_id=? GROUP BY channel,state", rs -> {
            result.computeIfAbsent(rs.getString(1), c -> new LinkedHashMap<>()).put(rs.getString(2), rs.getLong(3));
        }, id);
        return result;
    }
}
