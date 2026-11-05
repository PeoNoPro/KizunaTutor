package com.kizuna.features.booking.service.impl;
import com.kizuna.features.booking.repository.BookingActivityReportRepository;
import com.kizuna.features.booking.service.BookingActivityReportService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.*;
@Service @RequiredArgsConstructor
public class BookingActivityReportServiceImpl implements BookingActivityReportService {
    private final BookingActivityReportRepository reports;
    @Override public Map<String,Long> countsCreatedBetween(OffsetDateTime start,OffsetDateTime end) {
        var result=new LinkedHashMap<String,Long>();reports.counts(start,end).forEach(row->result.put((String)row[0],((Number)row[1]).longValue()));return result;
    }
}
