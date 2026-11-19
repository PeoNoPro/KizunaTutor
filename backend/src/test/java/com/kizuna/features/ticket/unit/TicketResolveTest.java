package com.kizuna.features.ticket.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Giải quyết yêu cầu - SupportTicketCommandExecutor.command(id, "resolve", Command).
 * Căn cứ: spec 010 FR-008 (Đang xử lý -> Đã giải quyết kèm hướng giải quyết), FR-007 (1–10.000 ký tự), FR-013, FR-018.
 */
class TicketResolveTest extends TicketUnitTestBase {

    @BeforeEach
    void setUp() { when(messages.append(any(), any(), any())).thenReturn(UUID.randomUUID()); }

    @Test @DisplayName("UTCID01: Nhân viên hỗ trợ giải quyết yêu cầu Đang xử lý -> gửi hướng giải quyết cho người gửi, Đã giải quyết, ghi STATUS_CHANGE")
    void utcid01_resolve() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.command(TICKET, "resolve", resolve("Đã đặt lại mật khẩu, bạn đăng nhập lại nhé."));

        verify(messages).append(TICKET, CS, "Đã đặt lại mật khẩu, bạn đăng nhập lại nhé.");
        assertEquals("RESOLVED", t.get("status"));
        verify(audit).append(eq(TICKET), eq(CS), eq("STATUS_CHANGE"), eq(VERSION + 1), any(), any(), isNull());
        verify(notifier).command(t, "resolve", null);
    }

    @Test @DisplayName("UTCID02: Hướng giải quyết đúng 10.000 ký tự (biên trên) -> giải quyết được")
    void utcid02_text10000() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.command(TICKET, "resolve", resolve(chars(10_000)));
        assertEquals("RESOLVED", t.get("status"));
    }

    @Test @DisplayName("UTCID03: Hướng giải quyết 10.001 ký tự (vượt biên) -> 400")
    void utcid03_text10001() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.command(TICKET, "resolve", resolve(chars(10_001)))).getMessage());
        assertEquals("IN_PROGRESS", t.get("status"));
        nothingSaved();
    }

    @Test @DisplayName("UTCID04: Không nhập hướng giải quyết -> 400")
    void utcid04_blankText() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.command(TICKET, "resolve", resolve("   "))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Yêu cầu còn Mới tiếp nhận (chưa nhận xử lý) -> 409")
    void utcid05_openTicket() {
        as(CS_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "resolve", resolve("Xong"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Yêu cầu đang chuyển quản lý (chờ quyết định) -> 409")
    void utcid06_escalatedTicket() {
        as(CS_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        fails(409, () -> executor.command(TICKET, "resolve", resolve("Xong")));
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Chủ yêu cầu tự bấm giải quyết -> 403")
    void utcid07_ownerCannotResolve() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "resolve", resolve("Xong"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Phiên bản đã cũ -> 409")
    void utcid08_staleVersion() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        var stale = new com.kizuna.features.ticket.dto.SupportTicketRequests.Command(VERSION - 1, null, null, null, null, null, null, "Xong");
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "resolve", stale)).getMessage());
        nothingSaved();
    }
}
