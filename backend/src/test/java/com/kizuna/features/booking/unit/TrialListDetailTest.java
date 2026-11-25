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
 * Unit test FE-07 / UC-07.05 View Trial Booking List - getMyTrialBookings(email, status, page, size)
 * và UC-07.06 View Trial Booking Details - getTrialBookingDetail(email, bookingId).
 */
class TrialListDetailTest extends TrialUnitTestBase {

    @Test @DisplayName("TC-07-037: Học viên xem danh sách -> buổi của mình, giờ học mới nhất trước, 10/trang")
    void tc07_037_learnerList() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 24);
        when(bookings.findByLearnerLearnerId(eq(learner.getLearnerId()), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(b), (Pageable) inv.getArgument(1), 1));

        var page = service.getMyTrialBookings(LEARNER_EMAIL, null, 0, 10);

        ArgumentCaptor<Pageable> p = ArgumentCaptor.forClass(Pageable.class);
        verify(bookings).findByLearnerLearnerId(eq(learner.getLearnerId()), p.capture());
        assertEquals(10, p.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "scheduledStartTime"), p.getValue().getSort());
        assertEquals(1, page.getTotalElements());
        assertEquals(BOOKING_ID, page.getContent().get(0).getBookingId());
    }

    @Test @DisplayName("TC-07-038: Lọc theo từng trạng thái -> truy vấn theo đúng trạng thái đó")
    void tc07_038_filterByStatus() {
        asLearner();
        when(bookings.findByLearnerLearnerIdAndBookingStatus(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        for (TrialBookingStatus s : List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED,
                TrialBookingStatus.COMPLETED, TrialBookingStatus.CANCELLED, TrialBookingStatus.DECLINED, TrialBookingStatus.EXPIRED)) {
            service.getMyTrialBookings(LEARNER_EMAIL, s.name().toLowerCase(), 0, 10);
            verify(bookings).findByLearnerLearnerIdAndBookingStatus(eq(learner.getLearnerId()), eq(s), any());
        }
    }

    @Test @DisplayName("TC-07-039: Trạng thái lạ 'ABC' hoặc 'ALL' -> trả tất cả buổi (không lọc)")
    void tc07_039_unknownStatus() {
        asLearner();
        when(bookings.findByLearnerLearnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getMyTrialBookings(LEARNER_EMAIL, "ABC", 0, 10);
        service.getMyTrialBookings(LEARNER_EMAIL, "ALL", 0, 10);

        verify(bookings, times(2)).findByLearnerLearnerId(eq(learner.getLearnerId()), any());
        verify(bookings, never()).findByLearnerLearnerIdAndBookingStatus(any(), any(), any());
    }

    @Test @DisplayName("TC-07-040: Gia sư xem danh sách -> chỉ buổi của gia sư đó")
    void tc07_040_tutorList() {
        asTutor();
        when(bookings.findByTutorTutorId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getMyTrialBookings(TUTOR_EMAIL, null, 0, 10);

        verify(bookings).findByTutorTutorId(eq(tutor.getTutorId()), any());
        verify(bookings, never()).findByLearnerLearnerId(any(), any());
        verify(learners, never()).findByUser(any());
    }

    @Test @DisplayName("TC-07-041: Một trong hai bên xem chi tiết -> trả giờ học, trạng thái, link phòng học")
    void tc07_041_detail() {
        asLearner();
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, 24);
        b.setMeetingLink("https://meet.google.com/x");

        var dto = service.getTrialBookingDetail(LEARNER_EMAIL, BOOKING_ID);

        assertEquals(BOOKING_ID, dto.getBookingId());
        assertEquals("https://meet.google.com/x", dto.getMeetingLink());
        assertEquals(b.getScheduledStartTime(), dto.getScheduledStartTime());
    }

    @Test @DisplayName("TC-07-042: Người ngoài xem chi tiết -> 403 'Bạn không có quyền xem thông tin buổi học thử này'")
    void tc07_042_strangerDetail() {
        asStranger("other@test.com");
        booking(TrialBookingStatus.CONFIRMED, 24);
        var ex = assertThrows(AccessDeniedException.class, () -> service.getTrialBookingDetail("other@test.com", BOOKING_ID));
        assertEquals("Bạn không có quyền xem thông tin buổi học thử này", ex.getMessage());
    }
}
