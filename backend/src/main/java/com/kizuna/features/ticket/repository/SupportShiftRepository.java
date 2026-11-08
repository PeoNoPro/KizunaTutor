package com.kizuna.features.ticket.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

/** Ca làm của nhân viên hỗ trợ (bảng support_shifts) và các truy vấn phục vụ việc tự giao yêu cầu theo ca. */
@Repository
public class SupportShiftRepository {
    private final JdbcTemplate jdbc;
    public SupportShiftRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> between(LocalDate from, LocalDate to) {
        return jdbc.queryForList("""
            SELECT s.shift_id, s.staff_id, u.full_name, s.shift_date, s.shift_code
            FROM support_shifts s JOIN users u ON u.user_id = s.staff_id
            WHERE s.shift_date BETWEEN ? AND ? ORDER BY s.shift_date, s.shift_code, u.full_name""", from, to);
    }

    /** Xếp ca và ghi lịch sử trong cùng giao dịch; trùng ca thì không ghi gì. */
    public boolean add(UUID id, UUID staff, LocalDate date, String code, UUID actor) {
        boolean added = jdbc.update("INSERT INTO support_shifts(shift_id,staff_id,shift_date,shift_code,created_by) VALUES(?,?,?,?,?) ON CONFLICT (staff_id,shift_date,shift_code) DO NOTHING",
            id, staff, date, code, actor) > 0;
        if (added) event(id, staff, date, code, "ASSIGN", actor);
        return added;
    }

    private void event(UUID shift, UUID staff, LocalDate date, String code, String action, UUID actor) {
        jdbc.update("INSERT INTO support_shift_events(event_id,shift_id,staff_id,shift_date,shift_code,action,actor_id) VALUES(?,?,?,?,?,?,?)",
            UUID.randomUUID(), shift, staff, date, code, action, actor);
    }

    /** Gỡ ca; dòng ca bị xóa nhưng lịch sử "gỡ ca" (ai gỡ, ca nào của ai) được giữ lại. */
    public boolean remove(UUID id, UUID actor) {
        var rows = jdbc.queryForList("DELETE FROM support_shifts WHERE shift_id=? RETURNING staff_id, shift_date, shift_code", id);
        if (rows.isEmpty()) return false;
        var row = rows.get(0);
        event(id, (UUID) row.get("staff_id"), ((java.sql.Date) row.get("shift_date")).toLocalDate(), row.get("shift_code").toString(), "REMOVE", actor);
        return true;
    }

    public Optional<LocalDate> dateOf(UUID id) {
        return jdbc.query("SELECT shift_date FROM support_shifts WHERE shift_id=? FOR UPDATE",(rs,n)->rs.getDate(1).toLocalDate(),id).stream().findFirst();
    }

    public boolean onShift(UUID staff, LocalDate date, String code) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM support_shifts WHERE staff_id=? AND shift_date=? AND shift_code=?)", Boolean.class, staff, date, code));
    }

    /** Ca sắp tới gần nhất của một nhân viên (từ hôm nay). */
    public List<Map<String, Object>> upcoming(UUID staff, LocalDate from, int limit) {
        return jdbc.queryForList("SELECT shift_date, shift_code FROM support_shifts WHERE staff_id=? AND shift_date>=? ORDER BY shift_date, CASE shift_code WHEN 'MORNING' THEN 1 WHEN 'AFTERNOON' THEN 2 ELSE 3 END LIMIT ?", staff, from, limit);
    }

    /**
     * Nhân viên hỗ trợ đang hoạt động trong ca, chưa giữ yêu cầu nào đang xử lý;
     * người lâu nhất chưa được giao xếp trước để chia đều.
     */
    public List<UUID> freeStaff(LocalDate date, String code) {
        return jdbc.queryForList("""
            SELECT s.staff_id FROM support_shifts s
            JOIN users u ON u.user_id=s.staff_id JOIN roles r ON r.role_id=u.role_id
            WHERE s.shift_date=? AND s.shift_code=? AND u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active=true AND r.role_name='ROLE_CS_STAFF'
              AND NOT EXISTS (SELECT 1 FROM support_tickets t WHERE t.assigned_to=s.staff_id AND t.status::text='IN_PROGRESS')
            ORDER BY (SELECT max(e.occurred_at) FROM ticket_audit_events e WHERE e.actor_user_id=s.staff_id AND e.event_type IN ('CLAIM','AUTO_ASSIGN')) NULLS FIRST, s.staff_id""",
            UUID.class, date, code);
    }

    /** Ngày đó Quản lý đã xếp ca nào chưa; chưa xếp thì không áp dụng luật nhận theo ca. */
    public boolean anyShiftOn(LocalDate date) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM support_shifts WHERE shift_date=?)", Boolean.class, date));
    }

    public long activeTickets(UUID staff) {
        return jdbc.queryForObject("SELECT count(*) FROM support_tickets WHERE assigned_to=? AND status::text='IN_PROGRESS'", Long.class, staff);
    }

    /** Yêu cầu mới cũ nhất chưa có người nhận; khóa dòng và bỏ qua dòng đang bị giao ở luồng khác. */
    public Optional<UUID> oldestOpenTicket() {
        var rows = jdbc.queryForList("SELECT ticket_id FROM support_tickets WHERE status::text='OPEN' AND assigned_to IS NULL ORDER BY created_at, ticket_id LIMIT 1 FOR UPDATE SKIP LOCKED", UUID.class);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<UUID> oldestUnavailableTicket() {
        return jdbc.queryForList("""
            SELECT t.ticket_id FROM support_tickets t
            WHERE t.status::text IN ('IN_PROGRESS','ESCALATED','RESOLVED') AND t.assigned_to IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.role_id=u.role_id
                WHERE u.user_id=t.assigned_to AND u.account_status::text='ACTIVE' AND u.deleted_at IS NULL
                  AND r.is_active=true AND r.role_name='ROLE_CS_STAFF')
            ORDER BY t.updated_at,t.ticket_id LIMIT 1 FOR UPDATE OF t SKIP LOCKED
            """, UUID.class).stream().findFirst();
    }

    public void lockDispatch() { jdbc.queryForList("SELECT pg_advisory_xact_lock(?)", 20261107L); }
}
