package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-08 / UC-08.01 Agree to Connect after Trial Lesson - TrialBookingServiceImpl.agreeConnectionAfterTrial(email, bookingId).
 */
class AgreeConnectionAfterTrialTest extends TrialUnitTestBase {

    @Test @DisplayName("TC-08-001: Đồng ý sau buổi học thử đã hoàn thành -> yêu cầu kết nối Accepted, đánh dấu đã đồng ý, hủy buổi học thử khác chưa diễn ra, báo học viên")
    void tc08_001_agree() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.COMPLETED, -2);
        TrialBooking otherOpen = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .scheduledStartTime(OffsetDateTime.now().plusDays(3)).bookingStatus(TrialBookingStatus.CONFIRMED).build();
        when(bookings.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(eq(tutor.getTutorId()), eq(learner.getLearnerId()), any()))
                .thenReturn(List.of(b, otherOpen));

        service.agreeConnectionAfterTrial(TUTOR_EMAIL, BOOKING_ID);

        assertEquals(ConnectionRequestStatus.ACCEPTED, b.getConnectionRequest().getStatus());
        assertTrue(b.getOfficialArrangementAgreed());
        assertEquals(TrialBookingStatus.CANCELLED, otherOpen.getBookingStatus());
        verify(events).publish(contains(":agreed"), eq("CONNECTION"), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("TC-08-001: Buổi học thử chưa gắn yêu cầu kết nối -> tạo yêu cầu kết nối Accepted mới")
    void tc08_001_createsConnectionIfMissing() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.COMPLETED, -2);
        b.setConnectionRequest(null);

        service.agreeConnectionAfterTrial(TUTOR_EMAIL, BOOKING_ID);

        verify(connections).save(argThat((ConnectionRequest cr) -> cr.getStatus() == ConnectionRequestStatus.ACCEPTED));
        assertNotNull(b.getConnectionRequest());
    }

    @Test @DisplayName("TC-08-002: Buổi học thử chưa hoàn thành -> 400 'Chỉ có thể đồng ý kết nối sau khi buổi học thử đã hoàn thành'")
    void tc08_002_notCompleted() {
        asTutor();
        booking(TrialBookingStatus.CONFIRMED, 24);
        var ex = assertThrows(BadRequestException.class, () -> service.agreeConnectionAfterTrial(TUTOR_EMAIL, BOOKING_ID));
        assertEquals("Chỉ có thể đồng ý kết nối sau khi buổi học thử đã hoàn thành", ex.getMessage());
    }

    @Test @DisplayName("TC-08-003: Gia sư hết gói hội viên -> 400 yêu cầu đăng ký / gia hạn gói")
    void tc08_003_noMembership() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.COMPLETED, -2);
        when(membership.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.agreeConnectionAfterTrial(TUTOR_EMAIL, BOOKING_ID));
        assertNotEquals(Boolean.TRUE, b.getOfficialArrangementAgreed());
    }

    @Test @DisplayName("TC-08-004: Đã đồng ý trước đó -> không đổi gì, không lưu")
    void tc08_004_alreadyAgreed() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.COMPLETED, -2);
        b.setOfficialArrangementAgreed(true);

        service.agreeConnectionAfterTrial(TUTOR_EMAIL, BOOKING_ID);

        verify(bookings, never()).save(any());
        verify(events, never()).publish(any(), any(), any(), any(), any(), any());
    }
}
