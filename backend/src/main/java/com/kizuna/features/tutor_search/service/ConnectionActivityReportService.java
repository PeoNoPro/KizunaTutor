package com.kizuna.features.tutor_search.service;
import java.time.OffsetDateTime;
import java.util.Map;
public interface ConnectionActivityReportService {
    Map<String,Long> countsCreatedBetween(OffsetDateTime start,OffsetDateTime end);
}
