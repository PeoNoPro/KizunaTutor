package com.kizuna.features.booking.service;
import java.time.OffsetDateTime;
import java.util.Map;
public interface BookingActivityReportService {
    Map<String,Long> countsCreatedBetween(OffsetDateTime start,OffsetDateTime end);
}
