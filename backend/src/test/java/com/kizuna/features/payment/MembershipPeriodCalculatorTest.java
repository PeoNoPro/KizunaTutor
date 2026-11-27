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

    // ===== REQ-15: ví dụ trong requirement =====

    @Test
    void req15_novemberPurchaseWithoutMembershipRunsToMidDecember() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 15), null, 1);

        assertEquals(LocalDate.of(2026, 11, 15), period.startDate());
        assertEquals(LocalDate.of(2026, 12, 14), period.endDate());
    }

    @Test
    void req15_purchaseStacksAfterMembershipEndingNovember30() {
        var period = calculator.calculate(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), 1);

        assertEquals(LocalDate.of(2026, 12, 1), period.startDate());
        assertEquals(LocalDate.of(2026, 12, 31), period.endDate());
    }

    @Test
    void req15_januaryThirtyFirstPlusOneMonthEndsInFebruary() {
        var period = calculator.calculate(LocalDate.of(2027, 1, 31), null, 1);

        assertEquals(LocalDate.of(2027, 1, 31), period.startDate());
        assertEquals(LocalDate.of(2027, 2, 27), period.endDate());
    }

    @Test
    void req15_januaryThirtyFirstInLeapYearEndsOnFebruary28() {
        assertEquals(LocalDate.of(2028, 2, 28), calculator.calculate(LocalDate.of(2028, 1, 31), null, 1).endDate());
    }

    @Test
    void req15_stackingAfterJanuary30StartsJanuary31AndHandlesShortFebruary() {
        var period = calculator.calculate(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 30), 1);

        assertEquals(LocalDate.of(2027, 1, 31), period.startDate());
        assertEquals(LocalDate.of(2027, 2, 27), period.endDate());
    }

    @Test
    void req15_twelveMonthPlanEndsTheDayBeforeTheSameDateNextYear() {
        assertEquals(LocalDate.of(2027, 11, 14), calculator.calculate(LocalDate.of(2026, 11, 15), null, 12).endDate());
    }

    @Test
    void rejectsInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDate.now(), null, 0));
    }
}
