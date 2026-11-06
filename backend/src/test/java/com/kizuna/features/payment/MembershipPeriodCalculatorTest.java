package com.kizuna.features.payment;

import com.kizuna.features.payment.service.MembershipPeriodCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FR-008: bắt đầu hôm nay hoặc cộng nối sau gói đang có; hết hạn = bắt đầu + n tháng − 1 ngày.
 */
class MembershipPeriodCalculatorTest {

    private final MembershipPeriodCalculator calculator = new MembershipPeriodCalculator();

    @Test
    void noActiveMembershipStartsToday() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 4), null, 1);
        assertEquals(LocalDate.of(2026, 11, 4), period.startDate());
        assertEquals(LocalDate.of(2026, 12, 3), period.endDate());
    }

    @Test
    void activeMembershipIsStackedAfterLatestEnd() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 4), LocalDate.of(2027, 2, 3), 3);
        assertEquals(LocalDate.of(2027, 2, 4), period.startDate());
        assertEquals(LocalDate.of(2027, 5, 3), period.endDate());
    }

    @Test
    void endedMembershipDoesNotDelayStart() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 3), 1);
        assertEquals(LocalDate.of(2026, 11, 4), period.startDate());
    }

    @Test
    void membershipEndingTodayStacksFromTomorrow() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4), 1);
        assertEquals(LocalDate.of(2026, 11, 5), period.startDate());
    }

    @Test
    void endOfMonthAndLeapYear() {
        assertEquals(LocalDate.of(2027, 2, 27), calculator.calculate(LocalDate.of(2027, 1, 31), null, 1).endDate());
        assertEquals(LocalDate.of(2028, 3, 28), calculator.calculate(LocalDate.of(2028, 2, 29), null, 1).endDate());
        assertEquals(LocalDate.of(2027, 5, 3), calculator.calculate(LocalDate.of(2026, 11, 4), null, 6).endDate());
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDate.now(), null, 0));
    }
}
