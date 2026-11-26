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
 * Unit test FE-07 / UC-07.05 View Trial Booking List - TrialBookingServiceImpl.getMyTrialBookings(email, status, page, size).
 */
class TrialListTest extends TrialUnitTestBase {

    @Test @DisplayName("UTCID01: Học viên xem danh sách -> buổi của mình, giờ học mới nhất trước, 10/trang")
    void utcid01_learnerList() {
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

    @Test @DisplayName("UTCID02: Lọc theo từng trạng thái -> truy vấn theo đúng trạng thái đó")
    void utcid02_filterByStatus() {
        asLearner();
        when(bookings.findByLearnerLearnerIdAndBookingStatus(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        for (TrialBookingStatus s : List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED,
                TrialBookingStatus.COMPLETED, TrialBookingStatus.CANCELLED, TrialBookingStatus.DECLINED, TrialBookingStatus.EXPIRED)) {
            service.getMyTrialBookings(LEARNER_EMAIL, s.name().toLowerCase(), 0, 10);
            verify(bookings).findByLearnerLearnerIdAndBookingStatus(eq(learner.getLearnerId()), eq(s), any());
        }
    }

    @Test @DisplayName("UTCID03: Trạng thái lạ 'ABC' -> trả tất cả buổi (không lọc)")
    void utcid03_unknownStatus() {
        asLearner();
        when(bookings.findByLearnerLearnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getMyTrialBookings(LEARNER_EMAIL, "ABC", 0, 10);

        verify(bookings).findByLearnerLearnerId(eq(learner.getLearnerId()), any());
        verify(bookings, never()).findByLearnerLearnerIdAndBookingStatus(any(), any(), any());
    }

    @Test @DisplayName("UTCID04: Trạng thái 'ALL' -> trả tất cả buổi (không lọc)")
    void utcid04_allStatus() {
        asLearner();
        when(bookings.findByLearnerLearnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getMyTrialBookings(LEARNER_EMAIL, "ALL", 0, 10);

        verify(bookings).findByLearnerLearnerId(eq(learner.getLearnerId()), any());
        verify(bookings, never()).findByLearnerLearnerIdAndBookingStatus(any(), any(), any());
    }

    @Test @DisplayName("UTCID05: Gia sư xem danh sách -> chỉ buổi của gia sư đó")
    void utcid05_tutorList() {
        asTutor();
        when(bookings.findByTutorTutorId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.getMyTrialBookings(TUTOR_EMAIL, null, 0, 10);

        verify(bookings).findByTutorTutorId(eq(tutor.getTutorId()), any());
        verify(bookings, never()).findByLearnerLearnerId(any(), any());
        verify(learners, never()).findByUser(any());
    }
}
