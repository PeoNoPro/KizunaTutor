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
 * Unit test FE-08 / UC-08.06 Cancel Connection Request - ConnectionRequestServiceImpl.cancel(email, requestId).
 */
class ConnectionCancelTest extends ConnectionUnitTestBase {

    @Test @DisplayName("UTCID01: Học viên hủy yêu cầu đang chờ -> Cancelled, báo gia sư")
    void utcid01_cancel() {
        asLearner();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 1, 2, "18:00");

        service.cancel(LEARNER_EMAIL, REQUEST_ID);

        assertEquals(ConnectionRequestStatus.CANCELLED, cr.getStatus());
        verify(events).publish(contains(":cancelled"), eq("CONNECTION"), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("UTCID02: Yêu cầu không còn Pending -> 400 'Chỉ có thể hủy yêu cầu kết nối đang chờ gia sư phản hồi'")
    void utcid02_notPending() {
        asLearner();
        ConnectionRequest cr = request(ConnectionRequestStatus.ACCEPTED, 1, 2, "18:00");
        var ex = assertThrows(BadRequestException.class, () -> service.cancel(LEARNER_EMAIL, REQUEST_ID));
        assertEquals("Chỉ có thể hủy yêu cầu kết nối đang chờ gia sư phản hồi", ex.getMessage());
        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
    }

    @Test @DisplayName("UTCID03: Học viên khác hủy yêu cầu -> 403")
    void utcid03_otherLearner() {
        User other = User.builder().userId(UUID.randomUUID()).email("b@test.com").role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        when(users.findByEmail("b@test.com")).thenReturn(Optional.of(other));
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 1, 2, "18:00");

        var ex = assertThrows(AccessDeniedException.class, () -> service.cancel("b@test.com", REQUEST_ID));
        assertEquals("Bạn không có quyền hủy yêu cầu kết nối này", ex.getMessage());
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }
}
