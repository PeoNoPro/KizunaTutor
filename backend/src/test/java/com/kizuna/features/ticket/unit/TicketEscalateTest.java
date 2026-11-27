package com.kizuna.features.ticket.unit;

import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Chuyển cấp cho quản lý - SupportTicketCommandExecutor.command(id, "escalate", Command).
 * Căn cứ: spec 010 FR-009 (quản lý nhận còn hoạt động, lý do và tóm tắt 1–10.000 ký tự), FR-010, FR-013, FR-018.
 */
class TicketEscalateTest extends TicketUnitTestBase {
    private static final String RECIPIENT_INVALID = "Người nhận không còn hoạt động hoặc không phù hợp.";

    @Test @DisplayName("UTCID01: Nhân viên hỗ trợ chuyển yêu cầu Đang xử lý cho quản lý còn hoạt động -> Đã chuyển quản lý, lưu lượt chuyển, ghi ESCALATE")
    void utcid01_escalate() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.command(TICKET, "escalate", escalate(MANAGER, "Cần hoàn tiền ngoài chính sách.", "Đã kiểm tra giao dịch, bị trừ 2 lần."));

        verify(actors).recipient(MANAGER, "ROLE_MANAGER");
        verify(tickets).escalate(TICKET, CS, MANAGER, "Cần hoàn tiền ngoài chính sách.", "Đã kiểm tra giao dịch, bị trừ 2 lần.");
        assertEquals("ESCALATED", t.get("status"));
        assertEquals(MANAGER, t.get("escalated_to"));
        assertEquals(true, t.get("is_escalated"));
        verify(audit).append(eq(TICKET), eq(CS), eq("ESCALATE"), eq(VERSION + 1), any(), any(), eq("Cần hoàn tiền ngoài chính sách."));
        verify(notifier).command(t, "escalate", null);
    }

    @Test @DisplayName("UTCID02: Lý do đúng 10.000 ký tự (biên trên) -> chuyển được")
    void utcid02_reason10000() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.command(TICKET, "escalate", escalate(MANAGER, chars(10_000), "Tóm tắt"));
        assertEquals("ESCALATED", t.get("status"));
    }

    @Test @DisplayName("UTCID03: Lý do 10.001 ký tự (vượt biên) -> 400, không chuyển")
    void utcid03_reason10001() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(INVALID_TEXT, fails(400, () -> executor.command(TICKET, "escalate", escalate(MANAGER, chars(10_001), "Tóm tắt"))).getMessage());
        verify(tickets, never()).escalate(any(), any(), any(), any(), any());
        nothingSaved();
    }

    @Test @DisplayName("UTCID04: Không nhập tóm tắt xử lý trước đó -> 400")
    void utcid04_blankSummary() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        fails(400, () -> executor.command(TICKET, "escalate", escalate(MANAGER, "Lý do", " ")));
        verify(tickets, never()).escalate(any(), any(), any(), any(), any());
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Không chọn quản lý nhận -> 400 'Người nhận không còn hoạt động hoặc không phù hợp.'")
    void utcid05_noManager() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        doThrow(new BusinessException(400, RECIPIENT_INVALID)).when(actors).recipient(isNull(), eq("ROLE_MANAGER"));
        assertEquals(RECIPIENT_INVALID, fails(400, () -> executor.command(TICKET, "escalate", escalate(null, "Lý do", "Tóm tắt"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Người nhận đã bị khóa hoặc không phải quản lý -> 400")
    void utcid06_inactiveManager() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        UUID locked = UUID.randomUUID();
        doThrow(new BusinessException(400, RECIPIENT_INVALID)).when(actors).recipient(locked, "ROLE_MANAGER");
        assertEquals(RECIPIENT_INVALID, fails(400, () -> executor.command(TICKET, "escalate", escalate(locked, "Lý do", "Tóm tắt"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Yêu cầu còn Mới tiếp nhận (chưa nhận xử lý) -> 409")
    void utcid07_openTicket() {
        as(CS_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "escalate", escalate(MANAGER, "Lý do", "Tóm tắt"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Yêu cầu đã chuyển quản lý rồi -> 409, không tạo lượt chuyển thứ hai")
    void utcid08_alreadyEscalated() {
        as(CS_ACTOR);
        ticket("ESCALATED", CS, MANAGER);
        fails(409, () -> executor.command(TICKET, "escalate", escalate(OTHER_MANAGER, "Lý do", "Tóm tắt")));
        verify(tickets, never()).escalate(any(), any(), any(), any(), any());
        nothingSaved();
    }

    @Test @DisplayName("UTCID09: Chủ yêu cầu tự chuyển cấp -> 403")
    void utcid09_ownerCannotEscalate() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "escalate", escalate(MANAGER, "Lý do", "Tóm tắt"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID10: Phiên bản đã cũ -> 409")
    void utcid10_staleVersion() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        var stale = new com.kizuna.features.ticket.dto.SupportTicketRequests.Command(VERSION - 1, null, MANAGER, null, "Lý do", "Tóm tắt", null, null);
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "escalate", stale)).getMessage());
        nothingSaved();
    }
}
