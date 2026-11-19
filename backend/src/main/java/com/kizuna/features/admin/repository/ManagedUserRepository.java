package com.kizuna.features.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * FE-11 (2026-11-13): Quản lý xem và tạm khóa / mở lại tài khoản Học viên, Gia sư.
 * Chỉ đọc các cột cần hiển thị; không đọc mật khẩu, tài liệu thẩm định hay dữ liệu thanh toán.
 * Dùng JdbcTemplate chính nên chạy chung transaction của service.
 */
@Repository
public class ManagedUserRepository {
    public static final Set<String> ROLES = Set.of("ROLE_LEARNER", "ROLE_TUTOR");
    private static final String BASE = """
        FROM users u JOIN roles r ON r.role_id=u.role_id
        LEFT JOIN tutors t ON t.user_id=u.user_id
        LEFT JOIN learners l ON l.user_id=u.user_id
        WHERE u.deleted_at IS NULL AND r.role_name IN ('ROLE_LEARNER','ROLE_TUTOR')""";
    private static final String COLUMNS = """
        SELECT u.user_id,u.full_name,u.email,coalesce(u.phone,t.phone) AS phone,r.role_name,u.account_status::text AS status,
               u.created_at,u.updated_at::text AS version,t.verification_status::text AS verification_status,
               t.tutor_id,l.learner_id""";
    private final JdbcTemplate jdbc;

    public ManagedUserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Filter(String q, String role, String status) {
        String where(List<Object> args) {
            var sql = new StringBuilder(BASE);
            if (role != null) { sql.append(" AND r.role_name=?"); args.add(role); }
            if (status != null) { sql.append(" AND u.account_status::text=?"); args.add(status); }
            if (q != null) {
                sql.append(" AND (u.full_name ILIKE ? ESCAPE '\\' OR u.email ILIKE ? ESCAPE '\\' OR coalesce(u.phone,t.phone,'') ILIKE ? ESCAPE '\\')");
                var like = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                args.add(like); args.add(like); args.add(like);
            }
            return sql.toString();
        }
    }

    public List<Map<String, Object>> list(Filter filter, int offset, int size) {
        var args = new ArrayList<Object>();
        var sql = COLUMNS + " " + filter.where(args) + " ORDER BY u.created_at DESC,u.user_id LIMIT ? OFFSET ?";
        args.add(size); args.add(offset);
        return jdbc.queryForList(sql, args.toArray());
    }

    public long count(Filter filter) {
        var args = new ArrayList<Object>();
        return jdbc.queryForObject("SELECT count(*) " + filter.where(args), Long.class, args.toArray());
    }

    /** Số liệu đầu trang: tổng học viên, gia sư, đang tạm khóa, tài khoản mới trong 7 ngày. */
    public Map<String, Object> summary() {
        return jdbc.queryForMap("""
            SELECT count(*) FILTER (WHERE r.role_name='ROLE_LEARNER') AS learners,
                   count(*) FILTER (WHERE r.role_name='ROLE_TUTOR') AS tutors,
                   count(*) FILTER (WHERE u.account_status::text IN ('SUSPENDED','BANNED')) AS suspended,
                   count(*) FILTER (WHERE u.created_at>=now()-interval '7 days') AS recent
            """ + BASE);
    }

    public Optional<Map<String, Object>> find(UUID id, boolean lock) {
        // FOR UPDATE OF u: khóa đúng dòng tài khoản để hai Quản lý không khóa/mở chồng lên nhau.
        return jdbc.queryForList(COLUMNS + ",l.current_level,l.target_level,t.membership_status::text AS membership_status,t.avg_rating,t.total_reviews "
                + BASE + " AND u.user_id=?" + (lock ? " FOR UPDATE OF u" : ""), id).stream().findFirst();
    }

    /** Hoạt động trên nền tảng (chỉ đếm), theo vai trò học viên hoặc gia sư. */
    public Map<String, Object> activity(UUID learnerId, UUID tutorId) {
        String side = learnerId != null ? "learner_id" : "tutor_id";
        UUID key = learnerId != null ? learnerId : tutorId;
        var out = new LinkedHashMap<String, Object>();
        out.put("connections", jdbc.queryForObject("SELECT count(*) FROM connection_requests WHERE " + side + "=?", Long.class, key));
        out.put("acceptedConnections", jdbc.queryForObject("SELECT count(*) FROM connection_requests WHERE status::text='ACCEPTED' AND " + side + "=?", Long.class, key));
        out.put("trials", jdbc.queryForObject("SELECT count(*) FROM trial_bookings WHERE " + side + "=?", Long.class, key));
        out.put("activeClasses", jdbc.queryForObject("SELECT count(*) FROM learning_spaces WHERE status::text='ACTIVE' AND " + side + "=?", Long.class, key));
        if (tutorId != null) {
            out.put("membershipEndDate", jdbc.queryForList("SELECT max(end_date) AS end_date FROM tutor_memberships WHERE tutor_id=? AND status::text='ACTIVE' AND end_date>=current_date", tutorId)
                    .get(0).get("end_date"));
        }
        return out;
    }

    public long ticketCount(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM support_tickets WHERE user_id=?", Long.class, user);
    }

    public void changeStatus(UUID id, String status) {
        jdbc.update("UPDATE users SET account_status=CAST(? AS user_account_status_enum),updated_at=now() WHERE user_id=?", status, id);
    }

    public void record(UUID actor, UUID target, String action, String reason) {
        jdbc.update("INSERT INTO user_account_moderation_events(event_id,actor_user_id,target_user_id,action,reason) VALUES (?,?,?,?,?)",
                UUID.randomUUID(), actor, target, action, reason);
    }

    /** Bảng nhật ký khóa/mở đã được tạo chưa (chưa chạy fe11_manager_user_management.sql thì chỉ ẩn lịch sử, không làm hỏng trang). */
    public boolean historyReady() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('user_account_moderation_events') IS NOT NULL", Boolean.class));
    }

    public List<Map<String, Object>> history(UUID target, int limit) {
        return jdbc.queryForList("""
            SELECT e.action,e.reason,e.occurred_at,a.full_name AS actor_name FROM user_account_moderation_events e
            JOIN users a ON a.user_id=e.actor_user_id WHERE e.target_user_id=? ORDER BY e.occurred_at DESC,e.event_id DESC LIMIT ?""", target, limit);
    }
}
