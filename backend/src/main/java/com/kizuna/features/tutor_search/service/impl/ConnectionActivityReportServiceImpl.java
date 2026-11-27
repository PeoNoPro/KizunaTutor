package com.kizuna.features.tutor_search.service.impl;
import com.kizuna.features.tutor_search.repository.ConnectionActivityReportRepository;
import com.kizuna.features.tutor_search.service.ConnectionActivityReportService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.*;
@Service @RequiredArgsConstructor
public class ConnectionActivityReportServiceImpl implements ConnectionActivityReportService {
    private final ConnectionActivityReportRepository reports;
    @Override public Map<String,Long> countsCreatedBetween(OffsetDateTime start,OffsetDateTime end) {
        var result=new LinkedHashMap<String,Long>(); reports.counts(start,end).forEach(row->result.put((String)row[0],((Number)row[1]).longValue()));return result;
    }
}
