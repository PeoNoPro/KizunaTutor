package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / BR-02 tự hết hạn yêu cầu học thử - TrialBookingServiceImpl.expireStalePendingTrials() (gọi từ BookingExpiryScheduler).
 */
class TrialAutoExpireTest extends TrialUnitTestBase {

    @Test @DisplayName("UTCID01: Tác vụ nền: yêu cầu chờ quá 24 giờ -> Expired, yêu cầu kết nối Expired, báo cả hai bên (BR-02)")
    void utcid01_autoExpire() {
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        when(bookings.findStalePending(eq(TrialBookingStatus.PENDING), any(), any())).thenReturn(List.of(b));

        int expired = service.expireStalePendingTrials();

        assertEquals(1, expired);
        assertEquals(TrialBookingStatus.EXPIRED, b.getBookingStatus());
        assertEquals(ConnectionRequestStatus.EXPIRED, b.getConnectionRequest().getStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.EXPIRED
                && h.getChangeReason().contains("24 giờ")));
        verify(events).publish(endsWith(":expired:learner"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
        verify(events).publish(endsWith(":expired:tutor"), any(), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }
}
