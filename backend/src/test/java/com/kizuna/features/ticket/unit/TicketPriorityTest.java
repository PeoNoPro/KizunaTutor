package com.kizuna.features.ticket.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-10: Đổi mức ưu tiên - SupportTicketCommandExecutor.command(id, "priority", Command).
 * Căn cứ: spec 010 FR-006 (thấp, bình thường, cao, khẩn cấp; lưu người và thời điểm đổi), FR-013, FR-017, FR-018.
 */
class TicketPriorityTest extends TicketUnitTestBase {

    @Test @DisplayName("UTCID01: Nhân viên hỗ trợ đổi Bình thường -> Khẩn cấp trên yêu cầu Đang xử lý -> lưu, ghi PRIORITY_CHANGE")
    void utcid01_raiseToUrgent() {
        as(CS_ACTOR);
        var t = ticket("IN_PROGRESS", CS, null);
        executor.command(TICKET, "priority", priority("URGENT"));

        assertEquals("URGENT", t.get("priority"));
        verify(tickets).save(t);
        verify(audit).append(eq(TICKET), eq(CS), eq("PRIORITY_CHANGE"), eq(VERSION + 1), any(), any(), isNull());
        verify(notifier).command(t, "priority", null);
    }

    @Test @DisplayName("UTCID02: Đổi sang Thấp trên yêu cầu Mới tiếp nhận chưa có người phụ trách -> lưu, ghi người phụ trách là nhân viên đang thao tác")
    void utcid02_lowOnOpenTicketAssignsActor() {
        as(CS_ACTOR);
        var t = ticket("OPEN", null, null);
        executor.command(TICKET, "priority", priority("LOW"));

        assertEquals("LOW", t.get("priority"));
        assertEquals(CS, t.get("assigned_to"));
        assertEquals("OPEN", t.get("status"));
    }

    @Test @DisplayName("UTCID03: Đổi ưu tiên khi yêu cầu đang chuyển quản lý -> vẫn được (nhân viên hỗ trợ theo dõi tiếp)")
    void utcid03_escalatedTicket() {
        as(CS_ACTOR);
        var t = ticket("ESCALATED", CS, MANAGER);
        executor.command(TICKET, "priority", priority("HIGH"));
        assertEquals("HIGH", t.get("priority"));
    }

    @Test @DisplayName("UTCID04: Mức ưu tiên không có trong danh mục ('CRITICAL') -> 400 'Lựa chọn chưa hợp lệ…'")
    void utcid04_unknownPriority() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals("Lựa chọn chưa hợp lệ. Vui lòng kiểm tra lại.", fails(400, () -> executor.command(TICKET, "priority", priority("CRITICAL"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID05: Không chọn mức ưu tiên (null) -> 400")
    void utcid05_missingPriority() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        fails(400, () -> executor.command(TICKET, "priority", priority(null)));
        nothingSaved();
    }

    @Test @DisplayName("UTCID06: Chủ yêu cầu tự đổi ưu tiên -> 403")
    void utcid06_ownerCannotChange() {
        as(LEARNER_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        assertEquals(NO_PERMISSION, fails(403, () -> executor.command(TICKET, "priority", priority("URGENT"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID07: Yêu cầu đã đóng -> 409")
    void utcid07_closed() {
        as(CS_ACTOR);
        ticket("CLOSED", CS, null);
        assertEquals(STATE_CHANGED, fails(409, () -> executor.command(TICKET, "priority", priority("HIGH"))).getMessage());
        nothingSaved();
    }

    @Test @DisplayName("UTCID08: Phiên bản đã cũ -> 409")
    void utcid08_staleVersion() {
        as(CS_ACTOR);
        ticket("IN_PROGRESS", CS, null);
        var stale = new com.kizuna.features.ticket.dto.SupportTicketRequests.Command(VERSION + 5, null, null, "HIGH", null, null, null, null);
        assertEquals(STALE, fails(409, () -> executor.command(TICKET, "priority", stale)).getMessage());
        nothingSaved();
    }
}
