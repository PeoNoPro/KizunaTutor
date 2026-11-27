package com.kizuna.features.ticket;

import com.kizuna.features.ticket.service.SupportTicketPriorityRule;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Mức ưu tiên ban đầu: buổi học thử sắp diễn ra là khẩn cấp; thanh toán/kỹ thuật cao; nội dung khác thấp. */
class SupportTicketPriorityRuleTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-09T10:00:00+07:00");

    @Test
    void bookingWithinTwentyFourHoursIsUrgentWhateverTheGroup() {
        assertEquals("URGENT", SupportTicketPriorityRule.initial("BOOKING", NOW.plusHours(3), NOW));
        assertEquals("URGENT", SupportTicketPriorityRule.initial("OTHER", NOW.plusHours(23), NOW));
    }

    @Test
    void pastOrFarBookingFallsBackToTheGroupRule() {
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("BOOKING", NOW.minusHours(1), NOW));
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("BOOKING", NOW.plusHours(24), NOW));
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("BOOKING", null, NOW));
    }

    @Test
    void groupsWithoutABookingGetTheirDefaultLevel() {
        assertEquals("HIGH", SupportTicketPriorityRule.initial("BILLING", null, NOW));
        assertEquals("HIGH", SupportTicketPriorityRule.initial("TECHNICAL", null, NOW));
        assertEquals("LOW", SupportTicketPriorityRule.initial("OTHER", null, NOW));
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("ACCOUNT", null, NOW));
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("MEMBERSHIP", null, NOW));
        assertEquals("MEDIUM", SupportTicketPriorityRule.initial("TUTOR", null, NOW));
    }
}
