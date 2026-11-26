package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-06 / UC-06.03 Turn Availability Slot On or Off - TutorScheduleServiceImpl.toggleAvailability(email, id, active).
 * Tắt ca đang mở chỉ được khi trong ca không có học thử đang chờ/đã xác nhận và không có buổi kết nối sắp tới.
 */
class AvailabilityToggleTest extends AvailabilityUnitTestBase {

    @Test @DisplayName("UTCID01: Tắt ca không có học viên -> lưu isActive = false")
    void utcid01_turnOff() {
        loggedInTutor();
        TutorAvailability slot = ownSlot(2, "18:00", "20:00", true);
        trialInSlot(false);
        connectionInSlot(false);

        var result = service.toggleAvailability(EMAIL, SLOT_ID, false);

        assertFalse(slot.getIsActive());
        assertFalse(result.getIsActive());
        verify(slots).save(slot);
    }

    @Test @DisplayName("UTCID02: Bật lại ca đang tắt -> lưu isActive = true, không cần kiểm tra học viên")
    void utcid02_turnOn() {
        loggedInTutor();
        TutorAvailability slot = ownSlot(2, "18:00", "20:00", false);

        service.toggleAvailability(EMAIL, SLOT_ID, true);

        assertTrue(slot.getIsActive());
        verify(trials, never()).hasActiveBookingsInSlot(any(), any(), any(), any());
        verify(connections, never()).hasUpcomingSessionsInSlot(any(), any(), any(), any());
    }

    @Test @DisplayName("UTCID03: Ca không thuộc gia sư -> 404 'Không tìm thấy ca rảnh cần cập nhật'")
    void utcid03_notOwned() {
        loggedInTutor();
        when(slots.findByAvailabilityIdAndTutor_TutorId(SLOT_ID, TUTOR_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.toggleAvailability(EMAIL, SLOT_ID, false));
        assertEquals("Không tìm thấy ca rảnh cần cập nhật", ex.getMessage());
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("UTCID04: Trong ca có học thử đang chờ hoặc đã xác nhận -> 409, ca vẫn mở")
    void utcid04_trialInSlot() {
        loggedInTutor();
        TutorAvailability slot = ownSlot(2, "18:00", "20:00", true);
        trialInSlot(true);

        var ex = assertThrows(ConflictException.class, () -> service.toggleAvailability(EMAIL, SLOT_ID, false));
        assertTrue(ex.getMessage().startsWith("Không thể tắt ca rảnh này vì đang có lịch học thử"));
        assertTrue(slot.getIsActive());
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("UTCID05: Trong ca có buổi kết nối đã đồng ý hoặc đang chờ còn hạn -> 409, ca vẫn mở")
    void utcid05_connectionInSlot() {
        loggedInTutor();
        TutorAvailability slot = ownSlot(2, "18:00", "20:00", true);
        trialInSlot(false);
        connectionInSlot(true);

        var ex = assertThrows(ConflictException.class, () -> service.toggleAvailability(EMAIL, SLOT_ID, false));
        assertTrue(ex.getMessage().startsWith("Không thể tắt ca rảnh này vì đang có học viên học hoặc chờ bạn duyệt"));
        assertTrue(slot.getIsActive());
        verify(slots, never()).save(any());
    }
}
