package com.kizuna.features.ticket.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Nhận xử lý yêu cầu - SupportTicketCommandExecutor.command(id, "claim", Command).
 * Căn cứ: spec 010 FR-005 (nhân viên nhận yêu cầu chưa có người phụ trách), FR-008, FR-013, FR-017, FR-018.
 */
class TicketClaimTest extends TicketUnitTestBase {

    @Test @DisplayName("UTCID01: Nhân viên hỗ trợ nhận yêu cầu Mới tiếp nhận -> người phụ trách = mình, Đang xử lý, ghi CLAIM, báo người gửi")
    void utcid01_claimOpenTicket() {
        as(CS_ACTOR);
        var t = ticket("OPEN", null, null);
        executor.command(TICKET, "claim", cmd(VERSION));

        assertEquals(CS, t.get("assigned_to"));
        assertEquals("IN_PROGRESS", t.get("status"));
        verify(tickets).save(t);
        verify(audit).append(eq(TICKET), eq(CS), eq("CLAIM"), eq(VERSION + 1), any(), any(), isNull());
        verify(notifier).claimed(t);
    }

    @Test @DisplayName("UTCID02: Yêu cầu đã Đang xử lý (đã có người nhận) -> 409, không nhận lần hai")
    void utcid02_alreadyClaimed() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "claim", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID03: Yêu cầu đã đóng -> 409")
    void utcid03_closed() {
        as(CS_ACTOR);
        ticket("CLOSED", CS, null);
        fails(409, () -> executor.command(TICKET, "claim", cmd(VERSION)));
        nothingSaved();
    }

    @Test @DisplayName("UTCID04: Chủ yêu cầu (học viên) tự nhận xử lý -> 403")
    void utcid04_ownerCannotClaim() {
        as(LEARNER_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "claim", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Quản lý không được chuyển cấp yêu cầu này -> 404")
    void utcid05_unrelatedManager() {
        as(MANAGER_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(NOT_FOUND, fails(404, () -> executor.command(TICKET, "claim", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Hai nhân viên cùng nhận, người sau gửi phiên bản cũ -> 409, chỉ người trước thành công")
    void utcid06_staleVersion() {
        as(CS_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "claim", cmd(VERSION - 1))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Phiên bản âm -> 400 'Vui lòng tải lại yêu cầu…'")
    void utcid07_negativeVersion() {
        as(CS_ACTOR);
        ticket("OPEN", null, null);
        assertEquals(RELOAD, fails(400, () -> executor.command(TICKET, "claim", cmd(-1L))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Yêu cầu không tồn tại -> 404")
    void utcid08_missingTicket() {
        as(CS_ACTOR);
        fails(404, () -> executor.command(TICKET, "claim", cmd(VERSION)));
        nothingSaved();
    }
}
