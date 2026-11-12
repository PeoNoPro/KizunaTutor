package com.kizuna.features.booking;

import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.PlannedSession;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklyBusy;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklySlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionSchedulePlannerTest {

    // Thứ Hai 16/11/2026
    private static final LocalDate MON = LocalDate.of(2026, 11, 16);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final int TUE = 3, WED = 4, FRI = 6;

    private static WeeklySlot slot(int dow, int h) {
        return new WeeklySlot(dow, LocalTime.of(h, 0), LocalTime.of(h + 1, 0));
    }

    @Test
    @DisplayName("Xếp đủ N buổi theo thứ tự thời gian qua nhiều tuần")
    void plansInOrder() {
        List<PlannedSession> s = ConnectionSchedulePlanner.plan(List.of(slot(FRI, 19), slot(TUE, 18)), MON, 5, NOW);
        assertEquals(5, s.size());
        assertEquals(List.of(MON.plusDays(1), MON.plusDays(4), MON.plusDays(8), MON.plusDays(11), MON.plusDays(15)),
                s.stream().map(PlannedSession::date).toList());
        assertEquals(LocalTime.of(18, 0), s.get(0).start());
        assertEquals(LocalTime.of(19, 0), s.get(1).start());
    }

    @Test
    @DisplayName("Bỏ qua buổi đã qua thời điểm hiện tại")
    void skipsPast() {
        LocalDateTime wedEvening = MON.plusDays(2).atTime(19, 30);
        List<PlannedSession> s = ConnectionSchedulePlanner.plan(List.of(slot(WED, 19)), MON, 2, wedEvening);
        assertEquals(List.of(MON.plusDays(9), MON.plusDays(16)), s.stream().map(PlannedSession::date).toList());
    }

    @Test
    @DisplayName("Buổi học thử ở tuần sau làm đúng buổi đó bị kín")
    void datedBusyHitsOnlyThatDate() {
        LocalDate nextWed = MON.plusDays(9);
        BusyIndex busy = new BusyIndex(List.of(),
                List.of(new DatedBusy(nextWed, LocalTime.of(19, 0), LocalTime.of(19, 30), BusyKind.TRIAL)));
        List<PlannedSession> s = ConnectionSchedulePlanner.plan(List.of(slot(WED, 19)), MON, 4, NOW);
        assertTrue(busy.conflictOf(s.get(0)).isEmpty());
        assertEquals(Optional.of(BusyKind.TRIAL), busy.conflictOf(s.get(1)));
        assertTrue(busy.conflictOf(s.get(2)).isEmpty());
        assertFalse(busy.allFree(s));
    }

    @Test
    @DisplayName("Lịch lớp hằng tuần làm mọi buổi cùng thứ bị kín")
    void weeklyBusyHitsEveryWeek() {
        BusyIndex busy = new BusyIndex(
                List.of(new WeeklyBusy(WED, LocalTime.of(18, 30), LocalTime.of(19, 30), BusyKind.TUTOR_CLASS)), List.of());
        List<PlannedSession> s = ConnectionSchedulePlanner.plan(List.of(slot(WED, 19)), MON, 3, NOW);
        assertTrue(s.stream().allMatch(x -> busy.conflictOf(x).isPresent()));
    }

    @Test
    @DisplayName("Giờ nối tiếp (kết thúc 19:00, bắt đầu 19:00) không tính là trùng")
    void adjacentIsNotConflict() {
        BusyIndex busy = new BusyIndex(List.of(),
                List.of(new DatedBusy(MON.plusDays(2), LocalTime.of(18, 30), LocalTime.of(19, 0), BusyKind.TRIAL)));
        List<PlannedSession> s = ConnectionSchedulePlanner.plan(List.of(slot(WED, 19)), MON, 1, NOW);
        assertTrue(busy.allFree(s));
    }

    @Test
    @DisplayName("Gợi ý tuần bắt đầu sớm nhất mà tất cả các buổi đều trống")
    void suggestsFirstFreeWeek() {
        LocalDate nextWed = MON.plusDays(9);
        BusyIndex busy = new BusyIndex(List.of(),
                List.of(new DatedBusy(nextWed, LocalTime.of(19, 0), LocalTime.of(19, 30), BusyKind.TRIAL)));
        Optional<LocalDate> week = ConnectionSchedulePlanner.suggestStartWeek(
                List.of(slot(WED, 19)), MON, 4, NOW, MON.plusWeeks(3), busy);
        assertEquals(Optional.of(MON.plusWeeks(2)), week);
    }

    @Test
    @DisplayName("Không có tuần nào phù hợp trong giới hạn thì không gợi ý")
    void noSuggestionWhenAlwaysBusy() {
        BusyIndex busy = new BusyIndex(
                List.of(new WeeklyBusy(WED, LocalTime.of(19, 0), LocalTime.of(20, 0), BusyKind.TUTOR_COURSE)), List.of());
        assertTrue(ConnectionSchedulePlanner.suggestStartWeek(
                List.of(slot(WED, 19)), MON, 4, NOW, MON.plusWeeks(3), busy).isEmpty());
    }
}
