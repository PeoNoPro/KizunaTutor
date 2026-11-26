package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-06 / UC-06.05 Delete Availability Slot - TutorScheduleServiceImpl.removeAvailability(email, id).
 */
class AvailabilityRemoveTest extends AvailabilityUnitTestBase {

    @Test @DisplayName("UTCID01: Xóa ca không có học viên -> xóa ca")
    void utcid01_deleteFreeSlot() {
        loggedInTutor();
        TutorAvailability slot = ownSlot(2, "18:00", "20:00", true);
        trialInSlot(false);
        connectionInSlot(false);

        service.removeAvailability(EMAIL, SLOT_ID);

        verify(slots).delete(slot);
    }

    @Test @DisplayName("UTCID02: Kiểm tra học viên theo đúng ngày và khung giờ của ca bị xóa")
    void utcid02_checksExactSlotRange() {
        loggedInTutor();
        ownSlot(4, "07:30", "09:00", true);
        trialInSlot(false);
        connectionInSlot(false);

        service.removeAvailability(EMAIL, SLOT_ID);

        verify(trials).hasActiveBookingsInSlot(TUTOR_ID, 4, LocalTime.of(7, 30), LocalTime.of(9, 0));
        verify(connections).hasUpcomingSessionsInSlot(TUTOR_ID, 4, LocalTime.of(7, 30), LocalTime.of(9, 0));
    }

    @Test @DisplayName("UTCID03: Ca không tồn tại hoặc không thuộc gia sư -> 404, không xóa")
    void utcid03_notOwned() {
        loggedInTutor();
        when(slots.findByAvailabilityIdAndTutor_TutorId(SLOT_ID, TUTOR_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.removeAvailability(EMAIL, SLOT_ID));
        assertEquals("Không tìm thấy ca rảnh cần xóa hoặc ca này không thuộc quyền sở hữu của bạn", ex.getMessage());
        verify(slots, never()).delete(any());
    }

    @Test @DisplayName("UTCID04: Trong ca có học thử -> 409, không xóa")
    void utcid04_trialInSlot() {
        loggedInTutor();
        ownSlot(2, "18:00", "20:00", true);
        trialInSlot(true);

        var ex = assertThrows(ConflictException.class, () -> service.removeAvailability(EMAIL, SLOT_ID));
        assertTrue(ex.getMessage().startsWith("Không thể xóa ca rảnh này vì đang có lịch học thử"));
        verify(slots, never()).delete(any());
    }

    @Test @DisplayName("UTCID05: Trong ca có buổi kết nối -> 409, không xóa")
    void utcid05_connectionInSlot() {
        loggedInTutor();
        ownSlot(2, "18:00", "20:00", true);
        trialInSlot(false);
        connectionInSlot(true);

        var ex = assertThrows(ConflictException.class, () -> service.removeAvailability(EMAIL, SLOT_ID));
        assertTrue(ex.getMessage().startsWith("Không thể xóa ca rảnh này vì đang có học viên học hoặc chờ bạn duyệt"));
        verify(slots, never()).delete(any());
    }
}
