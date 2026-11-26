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
 * Unit test FE-08 / BR-02 tự hết hạn yêu cầu kết nối - ConnectionRequestServiceImpl.expireStaleDirectRequests() (gọi từ BookingExpiryScheduler).
 */
class ConnectionAutoExpireTest extends ConnectionUnitTestBase {

    @Test @DisplayName("UTCID01: Tác vụ nền: yêu cầu chờ quá 24 giờ -> Expired, báo cả hai bên (BR-02)")
    void utcid01_autoExpire() {
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 25, 2, "18:00");
        when(requests.findByRequestTypeAndStatusAndCreatedAtBefore(eq(ConnectionRequestType.DIRECT), eq(ConnectionRequestStatus.PENDING), any()))
                .thenReturn(List.of(cr));

        assertEquals(1, service.expireStaleDirectRequests());

        assertEquals(ConnectionRequestStatus.EXPIRED, cr.getStatus());
        assertEquals("Tự động hết hạn vì gia sư chưa phản hồi trong 24 giờ", cr.getResponseMessage());
        verify(events).publish(endsWith(":expired:learner"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
        verify(events).publish(endsWith(":expired:tutor"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }
}
