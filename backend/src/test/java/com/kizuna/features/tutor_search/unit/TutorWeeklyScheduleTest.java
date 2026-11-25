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
 * Unit test FE-05 / UC-05.07 View Tutor Weekly Schedule.
 * - TutorSearchServiceImpl.getWeeklySchedule: tuần mặc định, giới hạn 2 tuần, gia sư không tồn tại.
 * - WeeklyScheduleCalculator.compute: trừ lịch bận / giờ đã qua, giữ khoảng đủ dài (30 phút học thử, 60 phút kết nối).
 * Thứ trong tuần: 1 = Chủ nhật, 2 = Thứ hai ... 7 = Thứ bảy. Phút tính từ 00:00.
 */
class TutorWeeklyScheduleTest extends TutorSearchUnitTestBase {
    private static final LocalDate THIS_MONDAY =
            LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // Tuần tương lai cố định để các test của bộ tính không phụ thuộc giờ chạy
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime BEFORE_WEEK = LocalDateTime.of(2030, 1, 1, 8, 0);

    private static int m(int h, int min) { return h * 60 + min; }

    private static List<Range> monday(List<DaySlots> days) { return days.get(0).freeRanges(); }

    // ---- getWeeklySchedule ----

    @Test @DisplayName("TC-05-039: Không chọn tuần -> trả tuần hiện tại (bắt đầu Thứ hai), đủ 7 ngày")
    void tc05_039_currentWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var week = service.getWeeklySchedule(TUTOR_ID, null, 30);
        assertEquals(THIS_MONDAY, week.getWeekStart());
        assertEquals(7, week.getDays().size());
    }

    @Test @DisplayName("TC-05-040: Chọn tuần sau -> trả tuần sau")
    void tc05_040_nextWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        assertEquals(THIS_MONDAY.plusWeeks(1), service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(1), 60).getWeekStart());
    }

    @Test @DisplayName("TC-05-041: Tuần sau nữa hoặc tuần trước -> 400 'Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này'")
    void tc05_041_outOfRange() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var ex = assertThrows(BadRequestException.class, () -> service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(2), 30));
        assertEquals("Chỉ có thể xem lịch rảnh trong 2 tuần tính từ tuần này", ex.getMessage());
        assertThrows(BadRequestException.class, () -> service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.minusWeeks(1), 30));
    }

    @Test @DisplayName("TC-05-044/045: Lịch bận lấy từ ConnectionBusyLoader cho đúng 7 ngày của tuần được xem")
    void tc05_044_loadsBusyForWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        service.getWeeklySchedule(TUTOR_ID, null, 30);
        verify(busyLoader).load(eq(TUTOR_ID), any(), any(), eq(THIS_MONDAY), eq(THIS_MONDAY.plusDays(6)));
    }

    @Test @DisplayName("TC-05-050: Gia sư không tồn tại -> 404")
    void tc05_050_tutorNotFound() {
        UUID random = UUID.randomUUID();
        when(repo.existsById(random)).thenReturn(false);
        var ex = assertThrows(ResourceNotFoundException.class, () -> service.getWeeklySchedule(random, null, 30));
        assertEquals("Không tìm thấy thông tin gia sư với mã yêu cầu", ex.getMessage());
    }

    @Test @DisplayName("TC-05-051: Gia sư không có ca rảnh -> cả tuần trống")
    void tc05_051_emptyWeek() {
        when(repo.existsById(TUTOR_ID)).thenReturn(true);
        var week = service.getWeeklySchedule(TUTOR_ID, THIS_MONDAY.plusWeeks(1), 30);
        assertTrue(week.getDays().stream().allMatch(d -> d.getFreeRanges().isEmpty()));
    }

    // ---- WeeklyScheduleCalculator.compute ----

    @Test @DisplayName("TC-05-042: Ca đang tắt không được truyền vào -> ngày đó không có giờ rảnh")
    void tc05_042_noActiveSlot() {
        var days = WeeklyScheduleCalculator.compute(MONDAY, List.of(), List.of(), List.of(), BEFORE_WEEK, 30);
        assertTrue(monday(days).isEmpty());
    }

    @Test @DisplayName("TC-05-043: Ca 18:00-21:00 trừ lịch lớp hằng tuần 19:00-20:00 -> còn 18:00-19:00 và 20:00-21:00")
    void tc05_043_classSchedule() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(21, 0))),
                List.of(new WeeklyRange(2, m(19, 0), m(20, 0))), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(19, 0)), new Range(m(20, 0), m(21, 0))), monday(days));
    }

    @Test @DisplayName("TC-05-044/045: Buổi kết nối / học thử đúng ngày 18:00-19:00 -> bị trừ chỉ ở ngày đó")
    void tc05_045_datedBusy() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(20, 0)), new WeeklyRange(3, m(18, 0), m(20, 0))),
                List.of(), List.of(new DatedRange(MONDAY, m(18, 0), m(19, 0))), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(19, 0), m(20, 0))), monday(days));
        assertEquals(List.of(new Range(m(18, 0), m(20, 0))), days.get(1).freeRanges(), "Thứ ba không bị ảnh hưởng");
    }

    @Test @DisplayName("TC-05-047: Hôm nay 18:10 -> giờ trước 18:30 (làm tròn lên mốc 30 phút) bị bỏ")
    void tc05_047_pastTimesToday() {
        LocalDateTime now = LocalDateTime.of(2030, 1, 7, 18, 10);
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(17, 0), m(20, 0))), List.of(), List.of(), now, 30);
        assertEquals(List.of(new Range(m(18, 30), m(20, 0))), monday(days));
    }

    @Test @DisplayName("TC-05-047: Ngày đã qua trong tuần -> không có giờ rảnh")
    void tc05_047_pastDay() {
        LocalDateTime wednesday = LocalDateTime.of(2030, 1, 9, 8, 0);
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(20, 0))), List.of(), List.of(), wednesday, 30);
        assertTrue(monday(days).isEmpty());
    }

    @Test @DisplayName("TC-05-048: Học thử (tối thiểu 30 phút): khoảng 30 phút hiện, khoảng 29 phút ẩn")
    void tc05_048_trialMinimum() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(18, 30)), new WeeklyRange(2, m(19, 0), m(19, 29))),
                List.of(), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(18, 30))), monday(days));
    }

    @Test @DisplayName("TC-05-049: Kết nối (tối thiểu 60 phút): khoảng 60 phút hiện, khoảng 59 phút ẩn")
    void tc05_049_connectionMinimum() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(8, 0), m(9, 0)), new WeeklyRange(2, m(10, 0), m(10, 59))),
                List.of(), List.of(), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(8, 0), m(9, 0))), monday(days));
    }

    @Test @DisplayName("TC-05-039: Hai ca chồng lên nhau trong cùng ngày được gộp thành một khoảng")
    void tc05_039_mergesOverlapping() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(19, 30)), new WeeklyRange(2, m(19, 0), m(21, 0))),
                List.of(), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(21, 0))), monday(days));
    }
}
