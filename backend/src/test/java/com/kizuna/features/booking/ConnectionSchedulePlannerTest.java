package com.kizuna.features.booking;

import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.PlannedSession;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklyBusy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Phát hiện trùng lịch của BusyIndex (lịch bận theo ngày cụ thể và theo thứ hằng tuần). */
class ConnectionSchedulePlannerTest {

    // Thứ Hai 16/11/2026
    private static final LocalDate MON = LocalDate.of(2026, 11, 16);
    private static final int WED = 4;

    /** Buổi Thứ Tư 19:00–20:00 của tuần thứ {@code week} tính từ MON. */
    private static PlannedSession wed19(int week) {
        LocalDate d = MON.plusDays(2).plusWeeks(week);
        return new PlannedSession(d, ConnectionSchedulePlanner.dayOfWeekOf(d), LocalTime.of(19, 0), LocalTime.of(20, 0));
    }

    @Test
    @DisplayName("Buổi học thử ở tuần sau làm đúng buổi đó bị kín")
    void datedBusyHitsOnlyThatDate() {
        BusyIndex busy = new BusyIndex(List.of(),
                List.of(new DatedBusy(wed19(1).date(), LocalTime.of(19, 0), LocalTime.of(19, 30), BusyKind.TRIAL)));
        assertTrue(busy.conflictOf(wed19(0)).isEmpty());
        assertEquals(Optional.of(BusyKind.TRIAL), busy.conflictOf(wed19(1)));
        assertTrue(busy.conflictOf(wed19(2)).isEmpty());
        assertFalse(busy.allFree(List.of(wed19(0), wed19(1), wed19(2))));
    }

    @Test
    @DisplayName("Lịch lớp hằng tuần làm mọi buổi cùng thứ bị kín")
    void weeklyBusyHitsEveryWeek() {
        BusyIndex busy = new BusyIndex(
                List.of(new WeeklyBusy(WED, LocalTime.of(18, 30), LocalTime.of(19, 30), BusyKind.TUTOR_CLASS)), List.of());
        assertTrue(List.of(wed19(0), wed19(1), wed19(2)).stream().allMatch(x -> busy.conflictOf(x).isPresent()));
    }

    @Test
    @DisplayName("Giờ nối tiếp (kết thúc 19:00, bắt đầu 19:00) không tính là trùng")
    void adjacentIsNotConflict() {
        BusyIndex busy = new BusyIndex(List.of(),
                List.of(new DatedBusy(wed19(0).date(), LocalTime.of(18, 30), LocalTime.of(19, 0), BusyKind.TRIAL)));
        assertTrue(busy.allFree(List.of(wed19(0))));
    }
}
