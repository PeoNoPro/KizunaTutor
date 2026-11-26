package com.kizuna.features.booking.unit;

import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.06 View Trial Booking Details - TrialBookingServiceImpl.getTrialBookingDetail(email, bookingId).
 */
class TrialDetailTest extends TrialUnitTestBase {

    @Test @DisplayName("UTCID01: Một trong hai bên xem chi tiết -> trả giờ học, trạng thái, link phòng học")
    void utcid01_detail() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 24);
        b.setMeetingLink("https://meet.google.com/x");

        var dto = service.getTrialBookingDetail(LEARNER_EMAIL, BOOKING_ID);

        assertEquals(BOOKING_ID, dto.getBookingId());
        assertEquals("https://meet.google.com/x", dto.getMeetingLink());
        assertEquals(b.getScheduledStartTime(), dto.getScheduledStartTime());
    }

    @Test @DisplayName("UTCID02: Người ngoài xem chi tiết -> 403 'Bạn không có quyền xem thông tin buổi học thử này'")
    void utcid02_strangerDetail() {
        asStranger("other@test.com");
        booking(TrialBookingStatus.CONFIRMED, 24);
        var ex = assertThrows(AccessDeniedException.class, () -> service.getTrialBookingDetail("other@test.com", BOOKING_ID));
        assertEquals("Bạn không có quyền xem thông tin buổi học thử này", ex.getMessage());
    }
}
