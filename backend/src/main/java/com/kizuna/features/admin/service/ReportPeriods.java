package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import java.time.*;
import java.time.temporal.ChronoUnit;
/** Khoảng ngày dùng chung cho tổng quan, thống kê và báo cáo (múi giờ Việt Nam, tối đa 366 ngày, mặc định 7 ngày gần nhất). */
public final class ReportPeriods {
    private ReportPeriods() { }
    public static Period period(LocalDate start,LocalDate end,OffsetDateTime now) {
        var zone=ZoneId.of("Asia/Ho_Chi_Minh"); var today=now.atZoneSameInstant(zone).toLocalDate();
        if((start==null)!=(end==null)) throw BusinessException.invalid();
        if(start==null) { start=today.minusDays(6); end=today; }
        if(start.isAfter(end) || end.isAfter(today) || ChronoUnit.DAYS.between(start,end)+1>366) throw BusinessException.invalid();
        var endExclusive=end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        var cutoff=endExclusive.isAfter(now)?now:endExclusive;
        return new Period(start.atStartOfDay(zone).toOffsetDateTime(),end,cutoff);
    }
    public record Period(OffsetDateTime start,LocalDate endDate,OffsetDateTime cutoff) { }
}
