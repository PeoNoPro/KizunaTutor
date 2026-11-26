package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.booking.dto.request.RescheduleTrialBookingRequestDTO;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.07 Reschedule Trial Lesson - rescheduleTrialBooking(email, bookingId, request).
 * Học viên dời -> chờ gia sư xác nhận lại (PENDING); gia sư dời -> chốt luôn (RESCHEDULED).
 * Buổi đã xác nhận chỉ dời được khi còn ít nhất 24 giờ.
 */
class TrialRescheduleTest extends TrialUnitTestBase {
    private static final OffsetDateTime NEW_START = OffsetDateTime.now().plusDays(5).withNano(0);

    private static RescheduleTrialBookingRequestDTO to(OffsetDateTime start) {
        return RescheduleTrialBookingRequestDTO.builder().newScheduledStartTime(start).rescheduleReason("Bận việc").build();
    }

    @Test @DisplayName("UTCID01: Học viên dời buổi đang chờ -> giờ mới, vẫn Pending, lịch sử, báo gia sư")
    void utcid01_learnerReschedulesPending() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);

        service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START));

        assertEquals(NEW_START, b.getScheduledStartTime());
        assertEquals(30, Duration.between(b.getScheduledStartTime(), b.getScheduledEndTime()).toMinutes());
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
        verify(history).save(any(TrialBookingHistory.class));
        verify(events).publish(contains(":rescheduled:"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("UTCID02: Học viên dời buổi đã xác nhận còn hơn 24 giờ -> về Pending, gia sư phải xác nhận lại")
    void utcid02_learnerReschedulesConfirmed() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 72);
        b.setTutorConfirmed(true);

        service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START));

        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
        assertFalse(b.getTutorConfirmed());
    }

    @Test @DisplayName("UTCID03: Gia sư dời buổi đã xác nhận -> Rescheduled (chốt luôn), báo học viên")
    void utcid03_tutorReschedules() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 72);

        service.rescheduleTrialBooking(TUTOR_EMAIL, BOOKING_ID, to(NEW_START));

        assertEquals(TrialBookingStatus.RESCHEDULED, b.getBookingStatus());
        verify(events).publish(contains(":rescheduled:"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("UTCID04: Gia sư dời sang giờ học viên đã có buổi đã xác nhận với gia sư khác -> 409")
    void utcid04_tutorReschedulesLearnerBusy() {
        asTutor();
        booking(TrialBookingStatus.CONFIRMED, 72);
        busyAt(NEW_START, BusyKind.LEARNER_COURSE);
        assertThrows(ConflictException.class, () -> service.rescheduleTrialBooking(TUTOR_EMAIL, BOOKING_ID, to(NEW_START)));
    }

    @Test @DisplayName("UTCID05: Buổi đã xác nhận còn dưới 24 giờ (23 giờ) -> 400")
    void utcid05_lessThan24h() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 23);
        var ex = assertThrows(BadRequestException.class, () -> service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START)));
        assertTrue(ex.getMessage().startsWith("Theo quy định, bạn chỉ có thể đổi lịch buổi học đã xác nhận trước giờ học ít nhất 24 giờ."));
        assertEquals(TrialBookingStatus.CONFIRMED, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID06: Buổi đang chờ dưới 24 giờ vẫn dời được (quy tắc 24 giờ chỉ áp cho buổi đã xác nhận)")
    void utcid06_pendingUnder24hAllowed() {
        asLearner();
        booking(TrialBookingStatus.PENDING, 5);
        service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START));
        verify(bookings).save(any(TrialBooking.class));
    }

    @Test @DisplayName("UTCID07: Giờ mới trong quá khứ -> 400 'Thời gian dời lịch học phải ở tương lai.'")
    void utcid07_pastNewTime() {
        asLearner();
        booking(TrialBookingStatus.PENDING, 48);
        var ex = assertThrows(BadRequestException.class,
                () -> service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(OffsetDateTime.now().minusHours(1))));
        assertEquals("Thời gian dời lịch học phải ở tương lai.", ex.getMessage());
    }

    @Test @DisplayName("UTCID08: Giờ mới trùng buổi học thử đã xác nhận của gia sư -> 409")
    void utcid08_conflict() {
        asLearner();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.existsConfirmedTutorOverlap(eq(tutor.getTutorId()), eq(BOOKING_ID), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START)));
        assertTrue(ex.getMessage().startsWith("Gia sư đã có một lịch hẹn học thử khác vào khung giờ mới này."));
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID09: Đề xuất ngoài lịch rảnh: 1 lần tạo + 1 lần dời trong 7 ngày -> lần dời thứ 2 bị chặn 409")
    void utcid09_offScheduleLimit() {
        asLearner();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        when(bookings.countOffScheduleSince(eq(learner.getLearnerId()), eq(tutor.getTutorId()), any())).thenReturn(1L);
        when(bookings.countOffScheduleReschedulesSince(eq(learner.getLearnerId()), eq(tutor.getTutorId()), any())).thenReturn(1L);

        var ex = assertThrows(ConflictException.class, () -> service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START)));
        assertTrue(ex.getMessage().startsWith("Bạn chỉ có thể đề xuất giờ ngoài lịch rảnh tối đa 2 lần mỗi tuần"));
    }

    @Test @DisplayName("UTCID10: Học viên dời ra ngoài lịch rảnh lần đầu -> được, lịch sử có dấu '[đề xuất ngoài lịch rảnh]'")
    void utcid10_offScheduleMarked() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);

        service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START));

        assertTrue(b.getOffSchedule());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getChangeReason().startsWith("[đề xuất ngoài lịch rảnh]")));
    }

    @Test @DisplayName("UTCID11: Buổi đã hủy -> 400 chỉ dời được buổi đang chờ hoặc đã xác nhận")
    void utcid11_cancelledBooking() {
        asLearner();
        booking(TrialBookingStatus.CANCELLED, 48);
        var ex = assertThrows(BadRequestException.class, () -> service.rescheduleTrialBooking(LEARNER_EMAIL, BOOKING_ID, to(NEW_START)));
        assertEquals("Chỉ có thể đổi lịch cho buổi học thử đang chờ xác nhận hoặc đã xác nhận", ex.getMessage());
    }

    @Test @DisplayName("UTCID12: Người ngoài dời lịch -> 403")
    void utcid12_stranger() {
        asStranger("other@test.com");
        booking(TrialBookingStatus.PENDING, 48);
        assertThrows(AccessDeniedException.class, () -> service.rescheduleTrialBooking("other@test.com", BOOKING_ID, to(NEW_START)));
    }
}
