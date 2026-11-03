package com.kizuna.features.admin.repository;

import com.kizuna.features.admin.dto.OperationsAccountSummaryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;

/** Tổng hợp chỉ đọc tài khoản dùng chung; không truy cập repository của feature khác. */
@Repository
public class OperationsAccountSummaryRepository {
    private final JdbcTemplate jdbc;
    public OperationsAccountSummaryRepository(@Qualifier("operationsIdentityJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OperationsAccountSummaryResponse read(OffsetDateTime generatedAt) {
        return jdbc.queryForObject("""
            WITH accounts AS (
                SELECT r.role_name, (u.account_status::text='ACTIVE' AND r.is_active) AS active,
                    r.role_name IN ('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACADEMIC_OFFICER','ROLE_CS_STAFF') AS staff
                FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL
            )
            SELECT count(*) AS total, count(*) FILTER (WHERE active) AS active_total,
                count(*) FILTER (WHERE role_name='ROLE_TUTOR') AS tutors,
                count(*) FILTER (WHERE role_name='ROLE_TUTOR' AND active) AS active_tutors,
                count(*) FILTER (WHERE role_name='ROLE_LEARNER') AS learners,
                count(*) FILTER (WHERE role_name='ROLE_LEARNER' AND active) AS active_learners,
                count(*) FILTER (WHERE staff) AS staff,
                count(*) FILTER (WHERE staff AND active) AS active_staff,
                count(*) FILTER (WHERE NOT staff AND role_name NOT IN ('ROLE_TUTOR','ROLE_LEARNER')) AS other
            FROM accounts
            """, (rs, row) -> new OperationsAccountSummaryResponse(rs.getLong("total"), rs.getLong("active_total"),
                rs.getLong("tutors"), rs.getLong("active_tutors"), rs.getLong("learners"), rs.getLong("active_learners"),
                rs.getLong("staff"), rs.getLong("active_staff"), rs.getLong("other"), generatedAt));
    }
}
