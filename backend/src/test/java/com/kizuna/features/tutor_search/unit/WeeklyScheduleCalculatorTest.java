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
 * Unit test FE-05 / UC-05.07 View Tutor Weekly Schedule - WeeklyScheduleCalculator.compute(weekStart, slots, weeklyBusy, datedBusy, now, minMinutes).
 * Trừ lịch bận / giờ đã qua, giữ khoảng đủ dài (30 phút học thử, 60 phút kết nối). Thứ: 1 = Chủ nhật, 2 = Thứ hai ... 7 = Thứ bảy; phút tính từ 00:00.
 */
class WeeklyScheduleCalculatorTest extends TutorSearchUnitTestBase {
    private static final LocalDate THIS_MONDAY =
            LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    // Tuần tương lai cố định để các test của bộ tính không phụ thuộc giờ chạy
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime BEFORE_WEEK = LocalDateTime.of(2030, 1, 1, 8, 0);

    private static int m(int h, int min) { return h * 60 + min; }

    private static List<Range> monday(List<DaySlots> days) { return days.get(0).freeRanges(); }

    @Test @DisplayName("UTCID01: Ca đang tắt không được truyền vào -> ngày đó không có giờ rảnh")
    void utcid01_noActiveSlot() {
        var days = WeeklyScheduleCalculator.compute(MONDAY, List.of(), List.of(), List.of(), BEFORE_WEEK, 30);
        assertTrue(monday(days).isEmpty());
    }

    @Test @DisplayName("UTCID02: Ca 18:00-21:00 trừ lịch lớp hằng tuần 19:00-20:00 -> còn 18:00-19:00 và 20:00-21:00")
    void utcid02_classSchedule() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(21, 0))),
                List.of(new WeeklyRange(2, m(19, 0), m(20, 0))), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(19, 0)), new Range(m(20, 0), m(21, 0))), monday(days));
    }

    @Test @DisplayName("UTCID03: Buổi kết nối / học thử đúng ngày 18:00-19:00 -> bị trừ chỉ ở ngày đó")
    void utcid03_datedBusy() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(20, 0)), new WeeklyRange(3, m(18, 0), m(20, 0))),
                List.of(), List.of(new DatedRange(MONDAY, m(18, 0), m(19, 0))), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(19, 0), m(20, 0))), monday(days));
        assertEquals(List.of(new Range(m(18, 0), m(20, 0))), days.get(1).freeRanges(), "Thứ ba không bị ảnh hưởng");
    }

    @Test @DisplayName("UTCID04: Hôm nay 18:10 -> giờ trước 18:30 (làm tròn lên mốc 30 phút) bị bỏ")
    void utcid04_pastTimesToday() {
        LocalDateTime now = LocalDateTime.of(2030, 1, 7, 18, 10);
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(17, 0), m(20, 0))), List.of(), List.of(), now, 30);
        assertEquals(List.of(new Range(m(18, 30), m(20, 0))), monday(days));
    }

    @Test @DisplayName("UTCID05: Ngày đã qua trong tuần -> không có giờ rảnh")
    void utcid05_pastDay() {
        LocalDateTime wednesday = LocalDateTime.of(2030, 1, 9, 8, 0);
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(20, 0))), List.of(), List.of(), wednesday, 30);
        assertTrue(monday(days).isEmpty());
    }

    @Test @DisplayName("UTCID06: Học thử (tối thiểu 30 phút): khoảng 30 phút hiện, khoảng 29 phút ẩn")
    void utcid06_trialMinimum() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(18, 30)), new WeeklyRange(2, m(19, 0), m(19, 29))),
                List.of(), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(18, 30))), monday(days));
    }

    @Test @DisplayName("UTCID07: Kết nối (tối thiểu 60 phút): khoảng 60 phút hiện, khoảng 59 phút ẩn")
    void utcid07_connectionMinimum() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(8, 0), m(9, 0)), new WeeklyRange(2, m(10, 0), m(10, 59))),
                List.of(), List.of(), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(8, 0), m(9, 0))), monday(days));
    }

    @Test @DisplayName("UTCID08: Hai ca chồng lên nhau trong cùng ngày được gộp thành một khoảng")
    void utcid08_mergesOverlapping() {
        var days = WeeklyScheduleCalculator.compute(MONDAY,
                List.of(new WeeklyRange(2, m(18, 0), m(19, 30)), new WeeklyRange(2, m(19, 0), m(21, 0))),
                List.of(), List.of(), BEFORE_WEEK, 30);
        assertEquals(List.of(new Range(m(18, 0), m(21, 0))), monday(days));
    }
}
