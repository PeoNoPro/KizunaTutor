package com.kizuna.features.notification.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Thông báo cá nhân (user_notifications nối notifications). Mọi câu lệnh đều lọc theo người nhận
 * nên không đọc hoặc sửa được bản của người khác; bản đã xóa mềm coi như không tồn tại.
 */
@Repository
public class NotificationInboxRepository {
    public record Item(UUID id, String title, String content, String type, OffsetDateTime receivedAt, boolean read, OffsetDateTime readAt, String actionPath) {}
    public record Preference(String type, boolean inApp, boolean email) {}

    private static final RowMapper<Item> ITEM = (rs, n) -> new Item(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
            rs.getObject(5, OffsetDateTime.class), rs.getBoolean(6), rs.getObject(7, OffsetDateTime.class), rs.getString(8));
    private static final String SELECT = "SELECT un.user_notification_id,n.title,n.content,n.notification_type::text,un.received_at,un.is_read,un.read_at,n.action_path"
            + " FROM user_notifications un JOIN notifications n ON n.notification_id=un.notification_id";

    private final JdbcTemplate jdbc;

    public NotificationInboxRepository(@Qualifier("notificationJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Account(UUID userId, String role, String fullName, long unread) {}

    /** Tài khoản còn hoạt động kèm vai trò, tên và số chưa đọc trong một truy vấn (DB ở xa nên mỗi lượt đi về đều tốn thời gian). */
    public Optional<Account> activeAccount(String email, UUID id) {
        return jdbc.query("SELECT u.user_id,r.role_name,u.full_name,(SELECT count(*) FROM user_notifications x WHERE x.user_id=u.user_id AND x.deleted_at IS NULL AND x.is_read=false)"
                + " FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active=true AND "
                + (id == null ? "u.email=?" : "u.user_id=?"),
                (rs, n) -> new Account(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getLong(4)), id == null ? email : id).stream().findFirst();
    }

    public record Page(List<Item> items, long total, long unread) {}

    /** Một trang danh sách cùng tổng số khớp bộ lọc và số chưa đọc, gộp trong một truy vấn; trang rỗng vẫn có số đếm. */
    public Page pageWithTotals(UUID user, boolean unreadOnly, String type, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        var args = new ArrayList<>();
        var filtered = "SELECT un.user_notification_id,n.title,n.content,n.notification_type::text AS type,un.received_at,un.is_read,un.read_at,n.action_path"
                + " FROM user_notifications un JOIN notifications n ON n.notification_id=un.notification_id" + where(unreadOnly, type, args, user)
                + (from == null ? "" : " AND un.received_at>=?") + (to == null ? "" : " AND un.received_at<?");
        if (from != null) args.add(from);
        if (to != null) args.add(to);
        args.add(user); args.add(size); args.add((long) page * size);
        var sql = "WITH f AS (" + filtered + ")"
                + " SELECT p.user_notification_id,p.title,p.content,p.type,p.received_at,p.is_read,p.read_at,p.action_path,t.total,t.unread FROM"
                + " (SELECT (SELECT count(*) FROM f) AS total,(SELECT count(*) FROM user_notifications WHERE user_id=? AND deleted_at IS NULL AND is_read=false) AS unread) t"
                + " LEFT JOIN LATERAL (SELECT * FROM f ORDER BY received_at DESC,user_notification_id DESC LIMIT ? OFFSET ?) p ON true";
        var items = new ArrayList<Item>();
        long[] totals = new long[2];
        jdbc.query(sql, rs -> {
            totals[0] = rs.getLong(9); totals[1] = rs.getLong(10);
            if (rs.getObject(1) != null) items.add(ITEM.mapRow(rs, items.size()));
        }, args.toArray());
        return new Page(items, totals[0], totals[1]);
    }

    private String where(boolean unreadOnly, String type, List<Object> args, UUID user) {
        args.add(user);
        var sql = new StringBuilder(" WHERE un.user_id=? AND un.deleted_at IS NULL");
        if (unreadOnly) sql.append(" AND un.is_read=false");
        if (type != null) { sql.append(" AND n.notification_type::text=?"); args.add(type); }
        return sql.toString();
    }

    public long unread(UUID user) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM user_notifications WHERE user_id=? AND deleted_at IS NULL AND is_read=false", Long.class, user));
    }

    public Optional<Item> find(UUID user, UUID id) {
        return jdbc.query(SELECT + " WHERE un.user_notification_id=? AND un.user_id=? AND un.deleted_at IS NULL", ITEM, id, user).stream().findFirst();
    }

    /** Đánh dấu đã đọc lần đầu; đọc lại không đổi thời điểm đọc. Trả false nếu không có bản (chưa xóa) của người này. */
    public boolean markRead(UUID user, UUID id) {
        var rows = jdbc.query("SELECT is_read FROM user_notifications WHERE user_notification_id=? AND user_id=? AND deleted_at IS NULL FOR UPDATE", (rs, n) -> rs.getBoolean(1), id, user);
        if (rows.isEmpty()) return false;
        if (!rows.get(0)) jdbc.update("UPDATE user_notifications SET is_read=true,read_at=now() WHERE user_notification_id=? AND is_read=false", id);
        return true;
    }

    /** Xóa mềm khỏi danh sách của người nhận. Xóa lặp vẫn đúng chủ; trả false nếu bản này không thuộc người đó. */
    public boolean softDelete(UUID user, UUID id) {
        var rows = jdbc.query("SELECT deleted_at IS NOT NULL FROM user_notifications WHERE user_notification_id=? AND user_id=? FOR UPDATE", (rs, n) -> rs.getBoolean(1), id, user);
        if (rows.isEmpty()) return false;
        if (!rows.get(0)) jdbc.update("UPDATE user_notifications SET deleted_at=now() WHERE user_notification_id=? AND deleted_at IS NULL", id);
        return true;
    }

    public long preferenceVersion(UUID user) {
        var rows = jdbc.query("SELECT version FROM notification_preference_state WHERE user_id=?", (rs, n) -> rs.getLong(1), user);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    public Map<String, Preference> preferences(UUID user) {
        var result = new HashMap<String, Preference>();
        jdbc.query("SELECT notification_type::text,in_app_enabled,email_enabled FROM notification_preferences WHERE user_id=?",
                rs -> { result.put(rs.getString(1), new Preference(rs.getString(1), rs.getBoolean(2), rs.getBoolean(3))); }, user);
        return result;
    }

    /** Khóa dòng phiên bản của người dùng (tạo nếu chưa có) và trả phiên bản hiện tại. */
    public long lockPreferenceVersion(UUID user) {
        jdbc.update("INSERT INTO notification_preference_state(user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING", user);
        return Objects.requireNonNull(jdbc.queryForObject("SELECT version FROM notification_preference_state WHERE user_id=? FOR UPDATE", Long.class, user));
    }

    public long savePreferences(UUID user, Collection<Preference> groups) {
        for (var group : groups) {
            jdbc.update("INSERT INTO notification_preferences(user_id,notification_type,in_app_enabled,email_enabled) VALUES (?,?::notification_type_enum,?,?)"
                    + " ON CONFLICT (user_id,notification_type) DO UPDATE SET in_app_enabled=EXCLUDED.in_app_enabled,email_enabled=EXCLUDED.email_enabled",
                    user, group.type(), group.inApp(), group.email());
        }
        return Objects.requireNonNull(jdbc.queryForObject("UPDATE notification_preference_state SET version=version+1,updated_at=now() WHERE user_id=? RETURNING version", Long.class, user));
    }
}
