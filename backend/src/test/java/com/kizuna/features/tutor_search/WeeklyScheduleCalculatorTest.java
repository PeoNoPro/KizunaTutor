package com.kizuna.features.tutor_search;

import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.DatedRange;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.DaySlots;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.Range;
import com.kizuna.features.tutor_search.service.impl.WeeklyScheduleCalculator.WeeklyRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyScheduleCalculatorTest {

    // Tuần 16/11/2026 (Thứ Hai) – 22/11/2026 (Chủ nhật)
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 16);
    private static final LocalDateTime BEFORE_WEEK = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final int FRI = 6;

    private static int m(int h, int min) {
        return h * 60 + min;
    }

    private static DaySlots day(List<DaySlots> days, LocalDate date) {
        return days.stream().filter(d -> d.date().equals(date)).findFirst().orElseThrow();
    }

    private static List<WeeklyRange> friEvening() {
        return List.of(new WeeklyRange(FRI, m(18, 0), m(21, 0)));
    }

    @Test
    @DisplayName("Trả đủ 7 ngày, Thứ Hai là dow 2, Chủ nhật là dow 1")
    void sevenDays() {
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, List.of(), List.of(), List.of(), BEFORE_WEEK, 60);
        assertEquals(7, days.size());
        assertEquals(2, days.get(0).dayOfWeek());
        assertEquals(1, days.get(6).dayOfWeek());
    }

    @Test
    @DisplayName("Lịch lớp / ca đã có học viên bị trừ khỏi lịch rảnh mọi tuần")
    void weeklyBusyRemoved() {
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(),
                List.of(new WeeklyRange(FRI, m(19, 0), m(20, 0))), List.of(), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(18, 0), m(19, 0)), new Range(m(20, 0), m(21, 0))),
                day(days, WEEK.plusDays(4)).freeRanges());
    }

    @Test
    @DisplayName("Buổi học thử cùng ngày bị ẩn")
    void trialSameDayHidden() {
        LocalDate fri = WEEK.plusDays(4);
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(), List.of(),
                List.of(new DatedRange(fri, m(18, 0), m(18, 30))), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(18, 30), m(21, 0))), day(days, fri).freeRanges());
    }

    @Test
    @DisplayName("Lịch bận ở tuần sau KHÔNG ảnh hưởng ngày tuần này (chỉ khóa đúng ngày)")
    void laterWeekBusyDoesNotHideThisWeek() {
        LocalDate nextFri = WEEK.plusDays(11);
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(), List.of(),
                List.of(new DatedRange(nextFri, m(19, 0), m(19, 30))), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(18, 0), m(21, 0))), day(days, WEEK.plusDays(4)).freeRanges());
    }

    @Test
    @DisplayName("Buổi học thử ở tuần trước không ảnh hưởng tuần sau")
    void trialEarlierWeekDoesNotHideLaterWeek() {
        LocalDate prevFri = WEEK.minusDays(3);
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(), List.of(),
                List.of(new DatedRange(prevFri, m(18, 0), m(21, 0))), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(18, 0), m(21, 0))), day(days, WEEK.plusDays(4)).freeRanges());
    }

    @Test
    @DisplayName("Buổi học thử khác thứ không ảnh hưởng")
    void trialOtherWeekdayIgnored() {
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(), List.of(),
                List.of(new DatedRange(WEEK.plusDays(3), m(18, 0), m(21, 0))), BEFORE_WEEK, 60);
        assertEquals(1, day(days, WEEK.plusDays(4)).freeRanges().size());
    }

    @Test
    @DisplayName("Ngày đã qua không có giờ; hôm nay chỉ còn giờ sau thời điểm hiện tại")
    void pastTimesHidden() {
        LocalDate fri = WEEK.plusDays(4);
        List<WeeklyRange> av = List.of(
                new WeeklyRange(2, m(18, 0), m(21, 0)),
                new WeeklyRange(FRI, m(18, 0), m(21, 0)));
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, av, List.of(), List.of(),
                fri.atTime(18, 10), 60);
        assertTrue(day(days, WEEK).freeRanges().isEmpty());
        assertEquals(List.of(new Range(m(18, 30), m(21, 0))), day(days, fri).freeRanges());
    }

    @Test
    @DisplayName("Khoảng trống ngắn hơn ca ngắn nhất thì không hiển thị")
    void shortGapsDropped() {
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, friEvening(),
                List.of(new WeeklyRange(FRI, m(18, 30), m(20, 30))), List.of(), BEFORE_WEEK, 60);
        assertTrue(day(days, WEEK.plusDays(4)).freeRanges().isEmpty());
    }

    @Test
    @DisplayName("Các khung rảnh chồng nhau được gộp lại")
    void overlappingAvailabilitiesMerged() {
        List<WeeklyRange> av = List.of(
                new WeeklyRange(FRI, m(18, 0), m(19, 30)),
                new WeeklyRange(FRI, m(19, 0), m(21, 0)));
        List<DaySlots> days = WeeklyScheduleCalculator.compute(WEEK, av, List.of(), List.of(), BEFORE_WEEK, 60);
        assertEquals(List.of(new Range(m(18, 0), m(21, 0))), day(days, WEEK.plusDays(4)).freeRanges());
    }
}
