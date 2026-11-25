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
 * Unit test FE-08 / UC-08.03 View Connection Requests - getMyDirectRequests(email, status, page, size),
 * UC-08.06 Cancel Connection Request - cancel(email, requestId) và BR-02 tự hết hạn - expireStaleDirectRequests().
 */
class ConnectionListCancelTest extends ConnectionUnitTestBase {

    // ---- UC-08.03 ----

    @Test @DisplayName("TC-08-021: Học viên xem yêu cầu -> chỉ yêu cầu DIRECT của mình, mới nhất trước, đúng số dòng mỗi trang")
    void tc08_021_learnerList() {
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

    @Test @DisplayName("TC-08-022: Lọc theo từng trạng thái -> truy vấn đúng trạng thái")
    void tc08_022_filter() {
        asLearner();
        when(requests.findByLearnerLearnerIdAndRequestTypeAndStatus(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        for (ConnectionRequestStatus s : List.of(ConnectionRequestStatus.PENDING, ConnectionRequestStatus.ACCEPTED,
                ConnectionRequestStatus.DECLINED, ConnectionRequestStatus.CANCELLED, ConnectionRequestStatus.EXPIRED)) {
            service.getMyDirectRequests(LEARNER_EMAIL, s.name(), 0, 20);
            verify(requests).findByLearnerLearnerIdAndRequestTypeAndStatus(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), eq(s), any());
        }
    }

    @Test @DisplayName("TC-08-023: Trạng thái lạ 'ABC' -> trả tất cả yêu cầu")
    void tc08_023_unknownStatus() {
        asLearner();
        when(requests.findByLearnerLearnerIdAndRequestType(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.getMyDirectRequests(LEARNER_EMAIL, "ABC", 0, 20);
        verify(requests).findByLearnerLearnerIdAndRequestType(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), any());
        verify(requests, never()).findByLearnerLearnerIdAndRequestTypeAndStatus(any(), any(), any(), any());
    }

    @Test @DisplayName("TC-08-021: Gia sư xem yêu cầu -> yêu cầu gửi tới gia sư đó")
    void tc08_021_tutorList() {
        asTutor();
        when(requests.findByTutorTutorIdAndRequestType(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        service.getMyDirectRequests(TUTOR_EMAIL, null, 0, 20);
        verify(requests).findByTutorTutorIdAndRequestType(eq(tutor.getTutorId()), eq(ConnectionRequestType.DIRECT), any());
    }

    // ---- UC-08.06 ----

    @Test @DisplayName("TC-08-035: Học viên hủy yêu cầu đang chờ -> Cancelled, báo gia sư")
    void tc08_035_cancel() {
        asLearner();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 1, 2, "18:00");

        service.cancel(LEARNER_EMAIL, REQUEST_ID);

        assertEquals(ConnectionRequestStatus.CANCELLED, cr.getStatus());
        verify(events).publish(contains(":cancelled"), eq("CONNECTION"), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("TC-08-036: Yêu cầu không còn Pending -> 400 'Chỉ có thể hủy yêu cầu kết nối đang chờ gia sư phản hồi'")
    void tc08_036_notPending() {
        asLearner();
        ConnectionRequest cr = request(ConnectionRequestStatus.ACCEPTED, 1, 2, "18:00");
        var ex = assertThrows(BadRequestException.class, () -> service.cancel(LEARNER_EMAIL, REQUEST_ID));
        assertEquals("Chỉ có thể hủy yêu cầu kết nối đang chờ gia sư phản hồi", ex.getMessage());
        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
    }

    @Test @DisplayName("TC-08-037: Học viên khác hủy yêu cầu -> 403")
    void tc08_037_otherLearner() {
        User other = User.builder().userId(UUID.randomUUID()).email("b@test.com").role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        when(users.findByEmail("b@test.com")).thenReturn(Optional.of(other));
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 1, 2, "18:00");

        var ex = assertThrows(AccessDeniedException.class, () -> service.cancel("b@test.com", REQUEST_ID));
        assertEquals("Bạn không có quyền hủy yêu cầu kết nối này", ex.getMessage());
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    // ---- BR-02 ----

    @Test @DisplayName("TC-08-020: Tác vụ nền: yêu cầu chờ quá 24 giờ -> Expired, báo cả hai bên (BR-02)")
    void tc08_020_autoExpire() {
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 25, 2, "18:00");
        when(requests.findByRequestTypeAndStatusAndCreatedAtBefore(eq(ConnectionRequestType.DIRECT), eq(ConnectionRequestStatus.PENDING), any()))
                .thenReturn(List.of(cr));

        assertEquals(1, service.expireStaleDirectRequests());

        assertEquals(ConnectionRequestStatus.EXPIRED, cr.getStatus());
        assertEquals("Tự động hết hạn vì gia sư chưa phản hồi trong 24 giờ", cr.getResponseMessage());
        verify(events).publish(endsWith(":expired:learner"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
        verify(events).publish(endsWith(":expired:tutor"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("TC-06-027: Ca chỉ có yêu cầu đã hết hạn -> hasUpcomingSessionsInSlot dựa vào truy vấn buổi sắp tới (không tính hết hạn)")
    void tc06_027_expiredRequestDoesNotBlock() {
        when(requests.existsUpcomingSessionInWeeklySlot(any(), anyInt(), any(), any())).thenReturn(false);
        assertFalse(service.hasUpcomingSessionsInSlot(tutor.getTutorId(), 2, java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0)));
        assertFalse(service.hasUpcomingSessionsInSlot(tutor.getTutorId(), null, java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0)));
    }
}
