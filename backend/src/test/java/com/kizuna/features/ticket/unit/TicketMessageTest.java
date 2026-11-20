package com.kizuna.features.ticket.unit;

import com.kizuna.features.ticket.dto.SupportTicketRequests.Message;
import com.kizuna.features.ticket.service.TicketAttachmentValidator.ValidatedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Gửi trao đổi trên yêu cầu hỗ trợ - SupportTicketCommandExecutor.message(id, Message, files).
 * Căn cứ: spec 010 FR-007 (trao đổi công khai 1–10.000 ký tự khi chưa đóng), FR-005, FR-017, FR-018, FR-020.
 */
class TicketMessageTest extends TicketUnitTestBase {

    @BeforeEach
    void setUp() { when(messages.append(any(), any(), any())).thenReturn(UUID.randomUUID()); }

    private static Message msg(String text) { return new Message(VERSION, text); }

    // ---------- Thành công (N) ----------

    @Test @DisplayName("UTCID01: Người gửi trả lời yêu cầu Đang xử lý -> lưu tin, giữ trạng thái, ghi lịch sử MESSAGE_SENT, báo nhân viên")
    void utcid01_ownerReplies() {
        as(LEARNER_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.message(TICKET, msg("Em đã thử lại nhưng vẫn lỗi."), List.of());

        verify(messages).append(TICKET, OWNER, "Em đã thử lại nhưng vẫn lỗi.");
        assertEquals("IN_PROGRESS", t.get("status"));
        verify(tickets).save(t);
        verify(audit).append(eq(TICKET), eq(OWNER), eq("MESSAGE_SENT"), eq(VERSION + 1), any(), any(), isNull());
        verify(notifier).message(t, true);
    }

    @Test @DisplayName("UTCID02: Người gửi trả lời yêu cầu Đã giải quyết -> mở lại thành Đang xử lý")
    void utcid02_ownerReopensResolved() {
        as(LEARNER_ACTOR);
        var t = ticket("RESOLVED", CS, null);
        executor.message(TICKET, msg("Vẫn chưa được ạ."), List.of());
        assertEquals("IN_PROGRESS", t.get("status"));
    }

    @Test @DisplayName("UTCID03: Nhân viên hỗ trợ trả lời yêu cầu Mới tiếp nhận -> tự nhận xử lý (người phụ trách = mình, Đang xử lý)")
    void utcid03_csReplyClaimsOpenTicket() {
        as(CS_ACTOR);
        var t = ticket("OPEN", null, null);
        executor.message(TICKET, msg("Chào bạn, mình kiểm tra ngay."), List.of());

        assertEquals(CS, t.get("assigned_to"));
        assertEquals("IN_PROGRESS", t.get("status"));
        verify(audit).append(eq(TICKET), eq(CS), eq("MESSAGE_SENT"), anyLong(), any(), any(), eq("Tự nhận xử lý khi trả lời"));
        verify(notifier).message(t, false);
    }

    @Test @DisplayName("UTCID04: Nhân viên hỗ trợ trả lời kèm 2 tệp -> lưu cả 2 tệp vào cùng tin nhắn")
    void utcid04_csReplyWithFiles() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        var files = List.of(new ValidatedFile("huong-dan.pdf", "application/pdf", new byte[]{1}), new ValidatedFile("anh.jpg", "image/jpeg", new byte[]{2}));
        executor.message(TICKET, msg("Bạn làm theo hướng dẫn đính kèm nhé."), files);
        verify(attachments, times(2)).save(eq(TICKET), any(), eq(CS), anyString(), anyString(), any());
    }

    // ---------- Biên (B) ----------

    @Test @DisplayName("UTCID05: Nội dung đúng 10.000 ký tự (biên trên) -> gửi được")
    void utcid05_text10000() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        executor.message(TICKET, msg(chars(10_000)), List.of());
        verify(messages).append(TICKET, OWNER, chars(10_000));
    }

    @Test @DisplayName("UTCID06: Nội dung 10.001 ký tự (vượt biên) -> 400")
    void utcid06_text10001() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.message(TICKET, msg(chars(10_001)), List.of())).getMessage());
        verify(messages, never()).append(any(), any(), any());
        nothingSaved();
    }

    // ---------- Bất thường (A) ----------

    @Test @DisplayName("UTCID07: Nội dung chỉ có khoảng trắng / xuống dòng -> 400")
    void utcid07_blankText() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.message(TICKET, msg(" \n\t "), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Phiên bản đã cũ (người khác vừa cập nhật) -> 409, không lưu")
    void utcid08_staleVersion() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(STALE, fails(409, () -> executor.message(TICKET, new Message(VERSION - 1, "Nội dung"), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID09: Không gửi phiên bản -> 400 'Vui lòng tải lại yêu cầu trước khi thực hiện thao tác.'")
    void utcid09_missingVersion() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(RELOAD, fails(400, () -> executor.message(TICKET, new Message(null, "Nội dung"), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID10: Yêu cầu đã đóng -> 409 'Trạng thái yêu cầu đã thay đổi…'")
    void utcid10_closedTicket() {
        as(LEARNER_ACTOR);
        ticket("CLOSED", CS, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.message(TICKET, msg("Nội dung"), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID11: Học viên khác (không phải chủ yêu cầu) -> 404, không lộ yêu cầu của người khác")
    void utcid11_otherLearner() {
        as(OTHER_LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(NOT_FOUND, fails(404, () -> executor.message(TICKET, msg("Nội dung"), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID12: Quản lý được chuyển cấp gửi trao đổi công khai -> 403 (quản lý chỉ lưu quyết định nội bộ)")
    void utcid12_managerCannotMessage() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.message(TICKET, msg("Nội dung"), List.of())).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID13: Yêu cầu không tồn tại -> 404")
    void utcid13_missingTicket() {
        as(LEARNER_ACTOR);
        assertEquals(NOT_FOUND, fails(404, () -> executor.message(TICKET, msg("Nội dung"), List.of())).getMessage());
        nothingSaved();
    }
}
