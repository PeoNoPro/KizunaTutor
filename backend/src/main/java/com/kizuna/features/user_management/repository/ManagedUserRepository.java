package com.kizuna.features.user_management.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FE-14: đọc tài khoản gia sư / học viên trên các bảng lõi (users, roles, tutors, learners), đổi đúng cột
 * users.account_status và ghi nhật ký vào system_audit_logs. Không tạo bảng, không sửa cột nào khác.
 */
@Repository
@RequiredArgsConstructor
public class ManagedUserRepository {

    public static final String AUDIT_ACTION = "USER_ACCOUNT_STATUS_CHANGE";

    private final JdbcTemplate jdbc;

    public record Account(UUID userId, String fullName, String email, String phone, String avatarUrl,
                          String roleName, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record AccountRow(UUID userId, String fullName, String email, String roleName, String status,
                             OffsetDateTime createdAt) {
    }

    public record TutorRow(Long tutorCode, String headline, Integer experienceYears, String highestJlptLevel,
                           BigDecimal avgRating, Integer totalReviews, String tutorState, String phone,
                           LocalDate dateOfBirth, String address, OffsetDateTime verifiedAt) {
    }

    public record LearnerRow(String currentLevel, String targetLevel, String learningGoals, String province,
                             String address) {
    }

    public record AuditRow(OffsetDateTime changedAt, String actorName, String fromStatus, String toStatus,
                           String reason, String messageToUser) {
    }

    public record Page(List<AccountRow> items, long total) {
    }

    private static final String ACCOUNT_FROM = " FROM users u JOIN roles r ON r.role_id = u.role_id WHERE u.deleted_at IS NULL";

    /** Danh sách theo vai trò, lọc trạng thái và từ khóa (họ tên, email), mới đăng ký trước. keyword đã được thoát ký tự LIKE. */
    public Page list(String roleName, String status, String likePattern, int page, int size) {
        StringBuilder where = new StringBuilder(ACCOUNT_FROM).append(" AND r.role_name = ?");
        List<Object> args = new ArrayList<>(List.of(roleName));
        if (status != null) {
            where.append(" AND u.account_status::text = ?");
            args.add(status);
        }
        if (likePattern != null) {
            where.append(" AND (u.full_name ILIKE ? ESCAPE '\\' OR u.email ILIKE ? ESCAPE '\\')");
            args.add(likePattern);
            args.add(likePattern);
        }
        Long total = jdbc.queryForObject("SELECT count(*)" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<AccountRow> items = jdbc.query(
                "SELECT u.user_id, u.full_name, u.email, r.role_name, u.account_status::text, u.created_at" + where
                        + " ORDER BY u.created_at DESC, u.user_id LIMIT ? OFFSET ?",
                (rs, i) -> new AccountRow(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getObject(6, OffsetDateTime.class)),
                pageArgs.toArray());
        return new Page(items, total == null ? 0 : total);
    }

    public Optional<Account> findAccount(UUID userId) {
        return jdbc.query("SELECT u.user_id, u.full_name, u.email, u.phone, u.avatar_url, r.role_name, "
                        + "u.account_status::text, u.created_at, u.updated_at" + ACCOUNT_FROM + " AND u.user_id = ?",
                (rs, i) -> new Account(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getObject(8, OffsetDateTime.class),
                        rs.getObject(9, OffsetDateTime.class)),
                userId).stream().findFirst();
    }

    public Optional<TutorRow> tutorProfile(UUID userId) {
        return jdbc.query("SELECT tutor_code, headline, experience_years, highest_jlpt_level, avg_rating, total_reviews, "
                        + "tutor_state, phone, date_of_birth, address, verified_at FROM tutors WHERE user_id = ?",
                (rs, i) -> new TutorRow(longOrNull(rs, 1), rs.getString(2), intOrNull(rs, 3), rs.getString(4),
                        rs.getBigDecimal(5), intOrNull(rs, 6), rs.getString(7), rs.getString(8),
                        rs.getObject(9, LocalDate.class), rs.getString(10), rs.getObject(11, OffsetDateTime.class)),
                userId).stream().findFirst();
    }

    public Optional<LearnerRow> learnerProfile(UUID userId) {
        return jdbc.query("SELECT current_level, target_level, learning_goals, province, address FROM learners WHERE user_id = ?",
                (rs, i) -> new LearnerRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                userId).stream().findFirst();
    }

    public List<AuditRow> statusHistory(UUID userId, int limit) {
        return jdbc.query("SELECT l.created_at, a.full_name, l.details->>'fromStatus', l.details->>'toStatus', "
                        + "l.details->>'reason', l.details->>'messageToUser' FROM system_audit_logs l "
                        + "LEFT JOIN users a ON a.user_id = l.actor_user_id "
                        + "WHERE l.action_type = ? AND l.target_id = ? ORDER BY l.created_at DESC, l.log_id LIMIT ?",
                (rs, i) -> new AuditRow(rs.getObject(1, OffsetDateTime.class), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6)),
                AUDIT_ACTION, userId, limit);
    }

    /** Đổi trạng thái nếu trạng thái hiện tại vẫn là expectedStatus. Trả số dòng đã đổi (0 = đã bị người khác đổi). */
    public int updateStatus(UUID userId, String expectedStatus, String newStatus) {
        return jdbc.update("UPDATE users SET account_status = ?::user_account_status_enum "
                        + "WHERE user_id = ? AND account_status = ?::user_account_status_enum AND deleted_at IS NULL",
                newStatus, userId, expectedStatus);
    }

    public void insertAudit(UUID actorId, UUID targetUserId, String detailsJson) {
        jdbc.update("INSERT INTO system_audit_logs (actor_user_id, action_type, target_entity, target_id, details) "
                + "VALUES (?, ?, 'users', ?, ?::jsonb)", actorId, AUDIT_ACTION, targetUserId, detailsJson);
    }

    private static Long longOrNull(ResultSet rs, int column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer intOrNull(ResultSet rs, int column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
