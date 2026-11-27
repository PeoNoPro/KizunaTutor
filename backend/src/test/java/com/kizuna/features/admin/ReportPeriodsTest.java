package com.kizuna.features.admin;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.service.ReportPeriods;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
/** Khoảng ngày dùng chung cho tổng quan, thống kê và báo cáo: múi giờ Việt Nam, tối đa 366 ngày, không có ngày tương lai. */
class ReportPeriodsTest {
    @Test void validatesVietnameseDateBoundaries() {
        var now=OffsetDateTime.parse("2026-03-01T18:00:00Z");
        var p=ReportPeriods.period(null,null,now);
        assertEquals(LocalDate.of(2026,3,2),p.endDate()); assertEquals(LocalDate.of(2026,2,24),p.start().toLocalDate());
        assertThrows(BusinessException.class,()->ReportPeriods.period(LocalDate.of(2024,1,1),LocalDate.of(2026,1,1),now));
        assertThrows(BusinessException.class,()->ReportPeriods.period(null,LocalDate.of(2026,1,1),now));
        assertDoesNotThrow(()->ReportPeriods.period(LocalDate.of(2025,3,2),LocalDate.of(2026,3,2),now));
        assertThrows(BusinessException.class,()->ReportPeriods.period(LocalDate.of(2025,3,1),LocalDate.of(2026,3,2),now));
        assertThrows(BusinessException.class,()->ReportPeriods.period(LocalDate.of(2026,3,3),LocalDate.of(2026,3,3),now));
    }
}
