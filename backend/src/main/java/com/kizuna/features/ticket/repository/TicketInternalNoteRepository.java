package com.kizuna.features.ticket.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi chú nội bộ của yêu cầu hỗ trợ (bảng ticket_internal_notes, database/seeds/fe10_ticket_internal_notes.sql).
 * Chỉ nhân viên hỗ trợ và Quản lý đọc/ghi; tách khỏi ticket_messages để không bao giờ lộ cho người gửi.
 */
@Repository
public class TicketInternalNoteRepository {
    /** Giới hạn số ghi chú trả về một lần (đủ cho một yêu cầu hỗ trợ). */
    public static final int MAX_NOTES = 300;

    private final JdbcTemplate jdbc;

    public TicketInternalNoteRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String, Object>> list(UUID ticket) {
        return jdbc.queryForList("SELECT n.note_id,n.note_text,n.created_at,n.author_id,u.full_name AS author_name,r.role_name AS author_role"
                + " FROM ticket_internal_notes n JOIN users u ON u.user_id=n.author_id JOIN roles r ON r.role_id=u.role_id"
                + " WHERE n.ticket_id=? ORDER BY n.created_at,n.note_id LIMIT " + MAX_NOTES, ticket);
    }

    public UUID add(UUID ticket, UUID author, String text) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ticket_internal_notes(note_id,ticket_id,author_id,note_text,created_at) VALUES(?,?,?,?,now())", id, ticket, author, text);
        return id;
    }
}
