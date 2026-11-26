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
 * Unit test FE-06 / UC-06.03, UC-06.05 - ConnectionRequestServiceImpl.hasUpcomingSessionsInSlot(tutorId, dayOfWeek, start, end): ca có buổi kết nối sắp tới hay không.
 */
class ConnectionSlotBusyTest extends ConnectionUnitTestBase {

    @Test @DisplayName("UTCID01: Ca chỉ có yêu cầu đã hết hạn -> hasUpcomingSessionsInSlot dựa vào truy vấn buổi sắp tới (không tính hết hạn)")
    void utcid01_expiredRequestDoesNotBlock() {
        when(requests.existsUpcomingSessionInWeeklySlot(any(), anyInt(), any(), any())).thenReturn(false);
        assertFalse(service.hasUpcomingSessionsInSlot(tutor.getTutorId(), 2, java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0)));
    }

    @Test @DisplayName("UTCID02: Không truyền thứ trong tuần (null) -> trả false, không lỗi")
    void utcid02_nullDay() {
        assertFalse(service.hasUpcomingSessionsInSlot(tutor.getTutorId(), null, java.time.LocalTime.of(18, 0), java.time.LocalTime.of(20, 0)));
    }
}
