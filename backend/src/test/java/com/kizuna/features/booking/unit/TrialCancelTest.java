package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.08 Cancel Trial Lesson - cancelTrialBooking(email, bookingId, request).
 * Học viên hủy buổi đã xác nhận phải trước giờ học ít nhất 24 giờ; gia sư không bị giới hạn này.
 */
class TrialCancelTest extends TrialUnitTestBase {

    private static CancelTrialBookingRequestDTO reason(String text) {
        return CancelTrialBookingRequestDTO.builder().cancelReason(text).build();
    }

    @Test @DisplayName("UTCID01: Học viên hủy buổi đang chờ -> Cancelled, yêu cầu kết nối Cancelled, lịch sử có lý do, báo gia sư")
    void utcid01_cancelPending() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);

        service.cancelTrialBooking(LEARNER_EMAIL, BOOKING_ID, reason("Changed plan"));

        assertEquals(TrialBookingStatus.CANCELLED, b.getBookingStatus());
        assertEquals(ConnectionRequestStatus.CANCELLED, b.getConnectionRequest().getStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.CANCELLED
                && "Changed plan".equals(h.getChangeReason())));
        verify(events).publish(contains(":cancelled"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("UTCID02: Học viên hủy buổi đã xác nhận còn 48 giờ -> hủy được")
    void utcid02_cancelConfirmedEarly() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 48);
        service.cancelTrialBooking(LEARNER_EMAIL, BOOKING_ID, reason("Sick"));
        assertEquals(TrialBookingStatus.CANCELLED, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID03: Học viên hủy buổi đã xác nhận còn 23 giờ -> 400 quy tắc 24 giờ")
    void utcid03_learnerLate() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 23);
        var ex = assertThrows(BadRequestException.class, () -> service.cancelTrialBooking(LEARNER_EMAIL, BOOKING_ID, reason("Sick")));
        assertEquals("Theo quy định, bạn chỉ có thể hủy buổi học thử đã xác nhận trước giờ học ít nhất 24 giờ.", ex.getMessage());
        assertEquals(TrialBookingStatus.CONFIRMED, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID04: Gia sư hủy buổi đã xác nhận còn 5 giờ -> hủy được, báo học viên")
    void utcid04_tutorLate() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 5);
        service.cancelTrialBooking(TUTOR_EMAIL, BOOKING_ID, reason("Emergency"));
        assertEquals(TrialBookingStatus.CANCELLED, b.getBookingStatus());
        verify(events).publish(contains(":cancelled"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("UTCID05: Lý do hủy trống -> 'Lý do hủy buổi học không được để trống' (kiểm tra trên DTO)")
    void utcid05_emptyReason() {
        assertTrue(hasError(reason(" "), "cancelReason", "Lý do hủy buổi học không được để trống"));
        assertFalse(hasError(reason("Sick"), "cancelReason", "Lý do hủy buổi học không được để trống"));
    }

    @Test @DisplayName("UTCID06: Buổi đã Completed / Declined / Expired / Cancelled -> 400 không hủy được")
    void utcid06_closedBooking() {
        asLearner();
        for (TrialBookingStatus closed : List.of(TrialBookingStatus.COMPLETED, TrialBookingStatus.DECLINED,
                TrialBookingStatus.EXPIRED, TrialBookingStatus.CANCELLED)) {
            booking(closed, 48);
            var ex = assertThrows(BadRequestException.class, () -> service.cancelTrialBooking(LEARNER_EMAIL, BOOKING_ID, reason("x")));
            assertEquals("Buổi học thử không thể hủy ở trạng thái hiện tại", ex.getMessage());
        }
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID07: Người ngoài hủy buổi học thử -> 403, không đổi")
    void utcid07_stranger() {
        asStranger("other@test.com");
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        var ex = assertThrows(AccessDeniedException.class, () -> service.cancelTrialBooking("other@test.com", BOOKING_ID, reason("x")));
        assertEquals("Bạn không có quyền hủy buổi học thử này", ex.getMessage());
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
    }
}
