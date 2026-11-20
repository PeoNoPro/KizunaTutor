package com.kizuna.features.ticket.unit;

import com.kizuna.features.ticket.dto.SupportTicketRequests.Note;
import com.kizuna.features.ticket.repository.TicketInternalNoteRepository;
import com.kizuna.features.ticket.service.SupportTicketProjection;
import com.kizuna.features.ticket.service.TicketAttachmentValidator;
import com.kizuna.features.ticket.service.impl.SupportTicketServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Thêm ghi chú nội bộ - SupportTicketServiceImpl.addNote(id, Note).
 * Căn cứ: spec 010 FR-012 (ghi chú nội bộ tách khỏi trao đổi công khai, chỉ nhân viên/quản lý thấy), FR-017.
 * Ghi chú tối đa 5.000 ký tự; yêu cầu đã đóng không thêm ghi chú.
 */
class TicketAddNoteTest extends TicketUnitTestBase {
    private final TicketInternalNoteRepository notes = mock(TicketInternalNoteRepository.class);
    private final SupportTicketServiceImpl service = new SupportTicketServiceImpl(notes, tickets, messages, attachments, audit, actors, policy,
            mock(SupportTicketProjection.class), contexts, executor, mock(TicketAttachmentValidator.class));
    private static final String STAFF_ONLY = "Ghi chú nội bộ chỉ dành cho bộ phận xử lý.";

    @Test @DisplayName("UTCID01: Nhân viên hỗ trợ thêm ghi chú trên yêu cầu Đang xử lý -> lưu ghi chú, trả danh sách ghi chú")
    void utcid01_csAddsNote() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        when(notes.list(TICKET)).thenReturn(List.of(Map.of("note_id", UUID.randomUUID(), "note_text", "Đã gọi điện cho học viên.", "author_role", "ROLE_CS_STAFF")));

        var result = service.addNote(TICKET, new Note("  Đã gọi điện cho học viên.  "));

        verify(notes).add(TICKET, CS, "Đã gọi điện cho học viên.");
        assertEquals(1, ((List<?>) result.get("items")).size());
    }

    @Test @DisplayName("UTCID02: Quản lý được chuyển cấp thêm ghi chú -> lưu được")
    void utcid02_managerAddsNote() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        service.addNote(TICKET, new Note("Chờ kế toán xác nhận."));
        verify(notes).add(TICKET, MANAGER, "Chờ kế toán xác nhận.");
    }

    @Test @DisplayName("UTCID03: Ghi chú đúng 5.000 ký tự (biên trên) -> lưu được")
    void utcid03_note5000() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        service.addNote(TICKET, new Note(chars(5_000)));
        verify(notes).add(TICKET, CS, chars(5_000));
    }

    @Test @DisplayName("UTCID04: Ghi chú 5.001 ký tự (vượt biên) -> 400")
    void utcid04_note5001() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> service.addNote(TICKET, new Note(chars(5_001)))).getMessage());
        verify(notes, never()).add(any(), any(), any());
    }

    @Test @DisplayName("UTCID05: Ghi chú chỉ có khoảng trắng -> 400")
    void utcid05_blankNote() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        fails(400, () -> service.addNote(TICKET, new Note("    ")));
        verify(notes, never()).add(any(), any(), any());
    }

    @Test @DisplayName("UTCID06: Chủ yêu cầu (học viên) thêm ghi chú nội bộ -> 403 'Ghi chú nội bộ chỉ dành cho bộ phận xử lý.'")
    void utcid06_ownerCannotAddNote() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(STAFF_ONLY, fails(403, () -> service.addNote(TICKET, new Note("Ghi chú"))).getMessage());
        verify(notes, never()).add(any(), any(), any());
    }

    @Test @DisplayName("UTCID07: Yêu cầu đã đóng -> 409 'Yêu cầu đã đóng, không thêm ghi chú được nữa.'")
    void utcid07_closedTicket() {
        as(CS_ACTOR);
        ticket("CLOSED", CS, null);
        assertEquals("Yêu cầu đã đóng, không thêm ghi chú được nữa.", fails(409, () -> service.addNote(TICKET, new Note("Ghi chú"))).getMessage());
        verify(notes, never()).add(any(), any(), any());
    }

    @Test @DisplayName("UTCID08: Quản lý không được chuyển cấp yêu cầu này -> 404")
    void utcid08_unrelatedManager() {
        as(OTHER_MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        assertEquals(NOT_FOUND, fails(404, () -> service.addNote(TICKET, new Note("Ghi chú"))).getMessage());
        verify(notes, never()).add(any(), any(), any());
    }

    @Test @DisplayName("UTCID09: Yêu cầu không tồn tại -> 404")
    void utcid09_missingTicket() {
        as(CS_ACTOR);
        fails(404, () -> service.addNote(TICKET, new Note("Ghi chú")));
        verify(notes, never()).add(any(), any(), any());
    }
}
