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
 * Unit test FE-07 / BR-03 tự hoàn thành buổi học thử - TrialBookingServiceImpl.completeEndedTrials() (gọi từ BookingExpiryScheduler).
 */
class TrialAutoCompleteTest extends TrialUnitTestBase {

    @Test @DisplayName("UTCID01: Tác vụ nền: buổi Confirmed/Rescheduled đã qua giờ kết thúc -> Completed kèm lịch sử (BR-03)")
    void utcid01_autoComplete() {
        TrialBooking b = booking(TrialBookingStatus.CONFIRMED, -2);
        when(bookings.findByBookingStatusInAndScheduledEndTimeBefore(
                eq(List.of(TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED)), any())).thenReturn(List.of(b));

        service.completeEndedTrials();

        assertEquals(TrialBookingStatus.COMPLETED, b.getBookingStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.COMPLETED));
    }
}
