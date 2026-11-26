package com.kizuna.features.booking.unit;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.ConnectionRequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-08 / UC-08.03 View Connection Requests - ConnectionRequestServiceImpl.getMyDirectRequests(email, status, page, size).
 */
class ConnectionListTest extends ConnectionUnitTestBase {

    @Test @DisplayName("UTCID01: Học viên xem yêu cầu -> chỉ yêu cầu DIRECT của mình, mới nhất trước, đúng số dòng mỗi trang")
    void utcid01_learnerList() {
        asLearner();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 1, 2, "18:00");
        when(requests.findByLearnerLearnerIdAndRequestType(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), any()))
                .thenReturn(new PageImpl<>(List.of(cr)));

        var page = service.getMyDirectRequests(LEARNER_EMAIL, null, 0, 20);

        ArgumentCaptor<Pageable> p = ArgumentCaptor.forClass(Pageable.class);
        verify(requests).findByLearnerLearnerIdAndRequestType(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), p.capture());
        assertEquals(20, p.getValue().getPageSize());
        assertTrue(p.getValue().getSort().getOrderFor("createdAt").isDescending());
        assertEquals(1, page.getContent().size());
    }

    @Test @DisplayName("UTCID02: Lọc theo từng trạng thái -> truy vấn đúng trạng thái")
    void utcid02_filter() {
        asLearner();
        when(requests.findByLearnerLearnerIdAndRequestTypeAndStatus(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        for (ConnectionRequestStatus s : List.of(ConnectionRequestStatus.PENDING, ConnectionRequestStatus.ACCEPTED,
                ConnectionRequestStatus.DECLINED, ConnectionRequestStatus.CANCELLED, ConnectionRequestStatus.EXPIRED)) {
            service.getMyDirectRequests(LEARNER_EMAIL, s.name(), 0, 20);
            verify(requests).findByLearnerLearnerIdAndRequestTypeAndStatus(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), eq(s), any());
        }
    }

    @Test @DisplayName("UTCID03: Trạng thái lạ 'ABC' -> trả tất cả yêu cầu")
    void utcid03_unknownStatus() {
        asLearner();
        when(requests.findByLearnerLearnerIdAndRequestType(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.getMyDirectRequests(LEARNER_EMAIL, "ABC", 0, 20);
        verify(requests).findByLearnerLearnerIdAndRequestType(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), any());
        verify(requests, never()).findByLearnerLearnerIdAndRequestTypeAndStatus(any(), any(), any(), any());
    }

    @Test @DisplayName("UTCID04: Gia sư xem yêu cầu -> yêu cầu gửi tới gia sư đó")
    void utcid04_tutorList() {
        asTutor();
        when(requests.findByTutorTutorIdAndRequestType(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.getMyDirectRequests(TUTOR_EMAIL, null, 0, 20);
        verify(requests).findByTutorTutorIdAndRequestType(eq(tutor.getTutorId()), eq(ConnectionRequestType.DIRECT), any());
    }
}
