package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.09 Confirm Trial Lesson Completed - TrialBookingServiceImpl.completeTrialBooking(email, bookingId).
 */
class TrialCompleteTest extends TrialUnitTestBase {

    @Test @DisplayName("UTCID01: Học viên xác nhận sau giờ kết thúc -> Completed, ghi bên xác nhận, lịch sử")
    void utcid01_complete() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, -1);

        service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID);

        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        assertTrue(b.getLearnerConfirmed());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.COMPLETED
                && h.getChangeReason().startsWith("Học viên")));
    }

    @Test @DisplayName("UTCID02: Buổi đã dời (Rescheduled) đã kết thúc -> gia sư xác nhận hoàn thành được")
    void utcid02_rescheduledByTutor() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.RESCHEDULED, -1);
        service.completeTrialBooking(TUTOR_EMAIL, BOOKING_ID);
        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        assertTrue(b.getTutorConfirmed());
    }

    @Test @DisplayName("UTCID03: Xác nhận trước giờ kết thúc -> 400 'Buổi học chưa kết thúc...'")
    void utcid03_notEnded() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 1);
        var ex = assertThrows(BadRequestException.class, () -> service.completeTrialBooking(TUTOR_EMAIL, BOOKING_ID));
        assertTrue(ex.getMessage().startsWith("Buổi học chưa kết thúc. Bạn chỉ có thể xác nhận hoàn thành sau"));
        assertEquals(TrialBookingStatus.CONFIRMED, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID04: Buổi đang chờ -> 400 'Chỉ có thể hoàn thành buổi học thử đã được xác nhận'")
    void utcid04_notConfirmed() {
        asLearner();
        booking(TrialBookingStatus.PENDING, -1);
        var ex = assertThrows(BadRequestException.class, () -> service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID));
        assertEquals("Chỉ có thể hoàn thành buổi học thử đã được xác nhận", ex.getMessage());
    }

    @Test @DisplayName("UTCID05: Buổi đã Completed -> không đổi gì, không lưu lại")
    void utcid05_alreadyCompleted() {
        asLearner();
        booking(TrialBookingStatus.COMPLETED, -1);
        service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID);
        verify(bookings, never()).save(any());
        verify(history, never()).save(any());
    }
}
