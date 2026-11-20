package com.kizuna.features.ticket.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Đóng yêu cầu - SupportTicketCommandExecutor.command(id, "close", Command).
 * Căn cứ: spec 010 FR-008 (chỉ chủ yêu cầu đóng khi đã giải quyết; đã đóng thì không thao tác tiếp), FR-013, FR-017, FR-018.
 */
class TicketCloseTest extends TicketUnitTestBase {

    @Test @DisplayName("UTCID01: Học viên đóng yêu cầu Đã giải quyết của mình -> Đã đóng, ghi CLOSE, báo nhân viên")
    void utcid01_learnerCloses() {
        as(LEARNER_ACTOR);
        var t = ticket("RESOLVED", CS, null);
        executor.command(TICKET, "close", cmd(VERSION));

        assertEquals("CLOSED", t.get("status"));
        verify(tickets).save(t);
        verify(audit).append(eq(TICKET), eq(OWNER), eq("CLOSE"), eq(VERSION + 1), any(), any(), isNull());
        verify(notifier).command(t, "close", null);
    }

    @Test @DisplayName("UTCID02: Gia sư đóng yêu cầu Đã giải quyết của mình -> Đã đóng")
    void utcid02_tutorCloses() {
        as(TUTOR_ACTOR);
        var t = ticket("RESOLVED", CS, null);
        executor.command(TICKET, "close", cmd(VERSION));
        assertEquals("CLOSED", t.get("status"));
    }

    @Test @DisplayName("UTCID03: Đóng khi yêu cầu còn Đang xử lý -> 409")
    void utcid03_notResolvedYet() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "close", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID04: Yêu cầu đã đóng rồi -> 409")
    void utcid04_alreadyClosed() {
        as(LEARNER_ACTOR);
        ticket("CLOSED", CS, null);
        fails(409, () -> executor.command(TICKET, "close", cmd(VERSION)));
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Nhân viên hỗ trợ đóng thay người gửi -> 403")
    void utcid05_csCannotClose() {
        as(CS_ACTOR);
        ticket("RESOLVED", CS, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "close", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Học viên khác đóng yêu cầu không phải của mình -> 404")
    void utcid06_otherLearner() {
        as(OTHER_LEARNER_ACTOR);
        ticket("RESOLVED", CS, null);
        assertEquals(NOT_FOUND, fails(404, () -> executor.command(TICKET, "close", cmd(VERSION))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Phiên bản đã cũ (nhân viên vừa trả lời thêm) -> 409")
    void utcid07_staleVersion() {
        as(LEARNER_ACTOR);
        ticket("RESOLVED", CS, null);
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "close", cmd(VERSION - 1))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Thao tác không tồn tại ('archive') -> 403, không đổi gì")
    void utcid08_unknownAction() {
        as(LEARNER_ACTOR);
        ticket("RESOLVED", CS, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "archive", cmd(VERSION))).getMessage());
        nothingSaved();
    }
}
