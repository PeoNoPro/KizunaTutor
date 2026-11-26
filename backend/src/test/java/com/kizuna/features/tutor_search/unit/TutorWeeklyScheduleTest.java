package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.DatedRange;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.DaySlots;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.Range;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.WeeklyRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-05 / UC-05.07 View Tutor Weekly Schedule - TutorSearchServiceImpl.getWeeklySchedule(tutorId, weekStart, minMinutes).
 * Tuần mặc định, giới hạn 2 tuần, gia sư không tồn tại.
 */
class TutorWeeklyScheduleTest extends TutorSearchUnitTestBase {
    private static final LocalDate THIS_MONDAY =
            LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // Tuần tương lai cố định để các test của bộ tính không phụ thuộc giờ chạy
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime BEFORE_WEEK = LocalDateTime.of(2030, 1, 1, 8, 0);

    private static int m(int h, int min) { return h * 60 + min; }

    private static List<Range> monday(List<DaySlots> days) { return days.get(0).freeRanges(); }

    @Test @DisplayName("UTCID01: Không chọn tuần -> trả tuần hiện tại (bắt đầu Thứ hai), đủ 7 ngày")
    void utcid01_currentWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var week = service.getWeeklySchedule(TUTOR_ID, null, 30);
        assertEquals(THIS_MONDAY, week.getWeekStart());
        assertEquals(7, week.getDays().size());
    }

    @Test @DisplayName("UTCID02: Chọn tuần sau -> trả tuần sau")
    void utcid02_nextWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        assertEquals(THIS_MONDAY.plusWeeks(1), service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(1), 60).getWeekStart());
    }

    @Test @DisplayName("UTCID03: Tuần sau nữa -> 400 'Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này'")
    void utcid03_twoWeeksAhead() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var ex = assertThrows(BadRequestException.class, () -> service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(2), 30));
        assertEquals("Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này", ex.getMessage());
    }

    @Test @DisplayName("UTCID04: Tuần trước -> 400 'Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này'")
    void utcid04_lastWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var ex = assertThrows(BadRequestException.class, () -> service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.minusWeeks(1), 30));
        assertEquals("Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này", ex.getMessage());
    }

    @Test @DisplayName("UTCID05: Lịch bận lấy từ ConnectionBusyLoader cho đúng 7 ngày của tuần được xem")
    void utcid05_loadsBusyForWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        service.getWeeklySchedule(TUTOR_ID, null, 30);
        verify(busyLoader).load(eq(TUTOR_ID), any(), any(), eq(THIS_MONDAY), eq(THIS_MONDAY.plusDays(6)));
    }

    @Test @DisplayName("UTCID06: Gia sư không tồn tại -> 404")
    void utcid06_tutorNotFound() {
        UUID random = UUID.randomUUID();
        when(repo.existsById(random)).thenReturn(false);
        var ex = assertThrows(ResourceNotFoundException.class, () -> service.getWeeklySchedule(random, null, 30));
        assertEquals("Không tìm thấy thông tin gia sư với mã yêu cầu", ex.getMessage());
    }

    @Test @DisplayName("UTCID07: Gia sư không có ca rảnh -> cả tuần trống")
    void utcid07_emptyWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var week = service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(1), 30);
        assertTrue(week.getDays().stream().allMatch(d -> d.getFreeRanges().isEmpty()));
    }
}
