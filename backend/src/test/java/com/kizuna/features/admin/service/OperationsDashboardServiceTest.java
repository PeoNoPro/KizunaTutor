package com.kizuna.features.admin.service;

import com.kizuna.features.ticket.service.SupportTicketSla;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Tổng quan Admin: tỷ lệ tăng giảm so với kỳ trước và thời hạn phản hồi theo mức ưu tiên. */
class OperationsDashboardServiceTest {
    @Test
    void changePercentComparesWithThePreviousPeriod() {
        assertEquals(50.0, OperationsDashboardService.trend(15, 10).get("changePercent"));
        assertEquals(-33.3, OperationsDashboardService.trend(2, 3).get("changePercent"));
        assertEquals(0.0, OperationsDashboardService.trend(4, 4).get("changePercent"));
        // Kỳ trước không có tài khoản mới: không chia cho 0, giao diện ghi "Mới".
        assertNull(OperationsDashboardService.trend(5, 0).get("changePercent"));
    }

    @Test
    void responseTargetDependsOnPriority() {
        var since = OffsetDateTime.parse("2026-11-12T08:00:00+07:00");
        assertEquals(since.plusHours(1), SupportTicketSla.dueAt("URGENT", since));
        assertEquals(since.plusHours(4), SupportTicketSla.dueAt("HIGH", since));
        assertEquals(since.plusHours(8), SupportTicketSla.dueAt("MEDIUM", since));
        assertEquals(since.plusHours(24), SupportTicketSla.dueAt("LOW", since));
        assertEquals(Duration.ofHours(24), SupportTicketSla.target("UNKNOWN"));
        assertNull(SupportTicketSla.dueAt("HIGH", null));
    }
}
