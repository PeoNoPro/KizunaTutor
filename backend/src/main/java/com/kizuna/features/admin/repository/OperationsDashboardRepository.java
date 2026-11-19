package com.kizuna.features.admin.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.*;

/** Số liệu cho Tổng quan Admin (bổ sung 2026-11-12): tài khoản mới kỳ này so với kỳ trước. */
@Repository
public class OperationsDashboardRepository {
    private final JdbcTemplate jdbc;

    public OperationsDashboardRepository(@Qualifier("operationsIdentityJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record NewAccounts(String role, long current, long previous) {}

    /** Tài khoản tạo mới theo vai trò trong kỳ [currentStart, currentEnd) và kỳ trước [previousStart, currentStart); không tính tài khoản đã xóa. */
    public List<NewAccounts> newAccounts(OffsetDateTime previousStart, OffsetDateTime currentStart, OffsetDateTime currentEnd) {
        return jdbc.query("""
                SELECT r.role_name,
                       count(*) FILTER (WHERE u.created_at>=? AND u.created_at<?),
                       count(*) FILTER (WHERE u.created_at>=? AND u.created_at<?)
                FROM users u JOIN roles r ON r.role_id=u.role_id
                WHERE u.deleted_at IS NULL AND u.created_at>=? AND u.created_at<?
                GROUP BY r.role_name""",
                (rs, n) -> new NewAccounts(rs.getString(1), rs.getLong(2), rs.getLong(3)),
                currentStart, currentEnd, previousStart, currentStart, previousStart, currentEnd);
    }
}
