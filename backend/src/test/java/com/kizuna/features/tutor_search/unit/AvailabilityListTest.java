package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-06 / UC-06.01 View My Availability Slots - TutorScheduleServiceImpl.getMyAvailabilities(email).
 */
class AvailabilityListTest extends AvailabilityUnitTestBase {

    @Test @DisplayName("UTCID01: Gia sư xem ca rảnh -> trả theo thứ tự repository (ngày, giờ bắt đầu), có tên ngày và giờ HH:mm")
    void utcid01_listOwnSlots() {
        loggedInTutor();
        TutorAvailability mon = TutorAvailability.builder().availabilityId(UUID.randomUUID()).tutor(tutor).dayOfWeek(2)
                .startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(20, 0)).isActive(true).build();
        TutorAvailability tue = TutorAvailability.builder().availabilityId(UUID.randomUUID()).tutor(tutor).dayOfWeek(3)
                .startTime(LocalTime.of(8, 30)).endTime(LocalTime.of(10, 0)).isActive(false).build();
        when(slots.findByTutor_TutorIdOrderByDayOfWeekAscStartTimeAsc(TUTOR_ID)).thenReturn(List.of(mon, tue));

        List<TutorAvailabilityDTO> result = service.getMyAvailabilities(EMAIL);

        assertEquals(2, result.size());
        assertEquals("Thứ hai", result.get(0).getDayName());
        assertEquals("18:00", result.get(0).getStartTime());
        assertEquals("20:00", result.get(0).getEndTime());
        assertTrue(result.get(0).getIsActive());
        assertEquals("08:30", result.get(1).getStartTime());
        assertFalse(result.get(1).getIsActive());
    }

    @Test @DisplayName("UTCID02: Tài khoản chưa liên kết hồ sơ gia sư -> 404 'Tài khoản của bạn chưa được liên kết với hồ sơ gia sư'")
    void utcid02_notLinked() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tutors.findByUser(user)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.getMyAvailabilities(EMAIL));
        assertEquals("Tài khoản của bạn chưa được liên kết với hồ sơ gia sư", ex.getMessage());
    }

    @Test @DisplayName("UTCID03: Gia sư chưa có ca rảnh -> danh sách rỗng")
    void utcid03_noSlot() {
        loggedInTutor();
        when(slots.findByTutor_TutorIdOrderByDayOfWeekAscStartTimeAsc(TUTOR_ID)).thenReturn(List.of());

        assertTrue(service.getMyAvailabilities(EMAIL).isEmpty());
    }
}
