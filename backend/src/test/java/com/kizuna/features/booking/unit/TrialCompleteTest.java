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
 * Unit test FE-07 / UC-07.09 Confirm Trial Lesson Completed - completeTrialBooking(email, bookingId),
 * BR-03 tự hoàn thành - completeEndedTrials() và BR-02 tự hết hạn - expireStalePendingTrials().
 */
class TrialCompleteTest extends TrialUnitTestBase {

    @Test @DisplayName("TC-07-058: Học viên xác nhận sau giờ kết thúc -> Completed, ghi bên xác nhận, lịch sử")
    void tc07_058_complete() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, -1);

        service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID);

        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        assertTrue(b.getLearnerConfirmed());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.COMPLETED
                && h.getChangeReason().startsWith("Học viên")));
    }

    @Test @DisplayName("TC-07-058: Buổi đã dời (Rescheduled) đã kết thúc -> gia sư xác nhận hoàn thành được")
    void tc07_058_rescheduledByTutor() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.RESCHEDULED, -1);
        service.completeTrialBooking(TUTOR_EMAIL, BOOKING_ID);
        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        assertTrue(b.getTutorConfirmed());
    }

    @Test @DisplayName("TC-07-059: Xác nhận trước giờ kết thúc -> 400 'Buổi học chưa kết thúc...'")
    void tc07_059_notEnded() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 1);
        var ex = assertThrows(BadRequestException.class, () -> service.completeTrialBooking(TUTOR_EMAIL, BOOKING_ID));
        assertTrue(ex.getMessage().startsWith("Buổi học chưa kết thúc. Bạn chỉ có thể xác nhận hoàn thành sau"));
        assertEquals(TrialBookingStatus.CONFIRMED, b.getBookingStatus());
    }

    @Test @DisplayName("TC-07-060: Buổi đang chờ -> 400 'Chỉ có thể hoàn thành buổi học thử đã được xác nhận'")
    void tc07_060_notConfirmed() {
        asLearner();
        booking(TrialBookingStatus.PENDING, -1);
        var ex = assertThrows(BadRequestException.class, () -> service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID));
        assertEquals("Chỉ có thể hoàn thành buổi học thử đã được xác nhận", ex.getMessage());
    }

    @Test @DisplayName("TC-07-061: Buổi đã Completed -> không đổi gì, không lưu lại")
    void tc07_061_alreadyCompleted() {
        asLearner();
        booking(TrialBookingStatus.COMPLETED, -1);
        service.completeTrialBooking(LEARNER_EMAIL, BOOKING_ID);
        verify(bookings, never()).save(any());
        verify(history, never()).save(any());
    }

    @Test @DisplayName("TC-07-062: Tác vụ nền: buổi Confirmed/Rescheduled đã qua giờ kết thúc -> Completed kèm lịch sử (BR-03)")
    void tc07_062_autoComplete() {
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, -2);
        when(bookings.findByBookingStatusInAndScheduledEndTimeBefore(
                eq(List.of(TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED)), any())).thenReturn(List.of(b));

        service.completeEndedTrials();

        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.COMPLETED));
    }

    @Test @DisplayName("TC-07-015: Tác vụ nền: yêu cầu chờ quá 24 giờ -> Expired, yêu cầu kết nối Expired, báo cả hai bên (BR-02)")
    void tc07_015_autoExpire() {
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        when(bookings.findStalePending(eq(TrialBookingStatus.PENDING), any(), any())).thenReturn(List.of(b));

        int expired = service.expireStalePendingTrials();

        assertEquals(1, expired);
        assertEquals(TrialBookingStatus.EXPIRED, b.getBookingStatus());
        assertEquals(ConnectionRequestStatus.EXPIRED, b.getConnectionRequest().getStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.EXPIRED
                && h.getChangeReason().contains("24 giờ")));
        verify(events).publish(endsWith(":expired:learner"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
        verify(events).publish(endsWith(":expired:tutor"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }
}
