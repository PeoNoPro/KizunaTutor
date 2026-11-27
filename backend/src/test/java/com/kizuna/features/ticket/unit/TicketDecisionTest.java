package com.kizuna.features.ticket.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Quản lý lưu hướng giải quyết - SupportTicketCommandExecutor.command(id, "decision", Command).
 * Căn cứ: spec 010 FR-010 (chỉ quản lý được chọn xử lý), FR-011 (nội dung 1–10.000 ký tự), FR-012, FR-013, FR-018.
 */
class TicketDecisionTest extends TicketUnitTestBase {

    @Test @DisplayName("UTCID01: Quản lý được chuyển cấp lưu hướng giải quyết -> lưu quyết định, giữ Đã chuyển quản lý, ghi SAVE_DECISION, báo nhân viên")
    void utcid01_saveDecision() {
        as(MANAGER_ACTOR);
        var t = ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        executor.command(TICKET, "decision", decision("Đồng ý hoàn tiền lần trừ thứ hai."));

        verify(tickets).decision(ESCALATION, MANAGER, "Đồng ý hoàn tiền lần trừ thứ hai.");
        assertEquals("ESCALATED", t.get("status"));
        verify(audit).append(eq(TICKET), eq(MANAGER), eq("SAVE_DECISION"), eq(VERSION + 1), any(), any(), eq("Đồng ý hoàn tiền lần trừ thứ hai."));
        verify(notifier).command(t, "decision", MANAGER);
    }

    @Test @DisplayName("UTCID02: Hướng giải quyết đúng 10.000 ký tự (biên trên) -> lưu được")
    void utcid02_text10000() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        executor.command(TICKET, "decision", decision(chars(10_000)));
        verify(tickets).decision(ESCALATION, MANAGER, chars(10_000));
    }

    @Test @DisplayName("UTCID03: Hướng giải quyết 10.001 ký tự (vượt biên) -> 400")
    void utcid03_text10001() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.command(TICKET, "decision", decision(chars(10_001)))).getMessage());
        verify(tickets, never()).decision(any(), any(), any());
        nothingSaved();
    }

    @Test @DisplayName("UTCID04: Không nhập hướng giải quyết -> 400")
    void utcid04_blankText() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        fails(400, () -> executor.command(TICKET, "decision", decision("")));
        verify(tickets, never()).decision(any(), any(), any());
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Quản lý khác (không được chuyển cấp) -> 404, không thấy yêu cầu")
    void utcid05_otherManager() {
        as(OTHER_MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        assertEquals(NOT_FOUND, fails(404, () -> executor.command(TICKET, "decision", decision("Đồng ý"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Nhân viên hỗ trợ đã đổi người nhận sang quản lý khác trong lúc đang soạn -> 403 'Bạn không còn phụ trách yêu cầu này.'")
    void utcid06_recipientChanged() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(OTHER_MANAGER);
        assertEquals("Bạn không còn phụ trách yêu cầu này.", fails(403, () -> executor.command(TICKET, "decision", decision("Đồng ý"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Không còn lượt chuyển cấp đang chờ (đã trả về) -> 409")
    void utcid07_noPendingEscalation() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        when(tickets.escalation(eq(TICKET), anyBoolean())).thenReturn(List.of());
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "decision", decision("Đồng ý"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Nhân viên hỗ trợ tự lưu quyết định thay quản lý -> 403")
    void utcid08_csCannotDecide() {
        as(CS_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "decision", decision("Đồng ý"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID09: Yêu cầu đã được trả về Đang xử lý -> 409")
    void utcid09_notEscalatedAnymore() {
        as(MANAGER_ACTOR);
        ticket("IN_PROGRESS", CS, MANAGER);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "decision", decision("Đồng ý"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID10: Phiên bản đã cũ -> 409")
    void utcid10_staleVersion() {
        as(MANAGER_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        pendingEscalation(MANAGER);
        var stale = new com.kizuna.features.ticket.dto.SupportTicketRequests.Command(VERSION - 1, null, null, null, null, null, "Đồng ý", null);
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "decision", stale)).getMessage());
        nothingSaved();
    }
}
