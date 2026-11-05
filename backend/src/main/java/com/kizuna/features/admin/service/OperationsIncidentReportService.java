package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.repository.OperationsIncidentReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class OperationsIncidentReportService {
    private final OperationsIncidentReportRepository reports;
    private final OperationsActorResolver actors;
    private final Clock operationsClock;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=3)
    public Map<String,Object> report(LocalDate start,LocalDate end,String metric,int page,int size,OffsetDateTime requestedCutoff) {
        actors.requireAdmin(); OperationsValidation.page(page,size);
        var period=period(start,end,OffsetDateTime.now(operationsClock));
        var cutoff=period.cutoff();
        if(requestedCutoff!=null) {
            if(requestedCutoff.isAfter(cutoff) || requestedCutoff.isBefore(period.start())) throw BusinessException.invalid();
            cutoff=requestedCutoff;
        }
        var rows=reports.asOf(period.start(),cutoff);
        var sets=new LinkedHashMap<String,List<Object[]>>();
        sets.put("created",rows.stream().filter(r->!instant(r[2]).isBefore(period.start().toInstant())).toList());
        sets.put("resolved",rows.stream().filter(r->Boolean.TRUE.equals(r[5])).toList());
        sets.put("open",rows.stream().filter(r->!"CLOSED".equals(r[3])).toList());
        var result=new LinkedHashMap<String,Object>(); result.put("startDate",period.start().toLocalDate()); result.put("endDate",period.endDate()); result.put("cutoff",cutoff);
        result.put("createdCount",sets.get("created").size()); result.put("resolvedCount",sets.get("resolved").size()); result.put("openAtCutoff",sets.get("open").size());
        var groups=new LinkedHashMap<String,Object>(); sets.forEach((k,v)->groups.put(k,breakdown(v))); result.put("breakdowns",groups);
        if(metric!=null) {
            OperationsValidation.member(metric,sets.keySet()); var selected=sets.get(metric); int offset=OperationsValidation.page(page,size);
            result.put("items",selected.stream().skip(offset).limit(size).map(r->Map.of("incidentId",r[0],"subject",r[1],"statusLabel",TechnicalIncidentService.STATUS_LABELS.get(r[3]),"severityLabel",TechnicalIncidentService.SEVERITY_LABELS.get(r[4]))).toList());
            result.put("page",page); result.put("size",size); result.put("totalElements",selected.size());
        }
        return result;
    }
    public static Period period(LocalDate start,LocalDate end,OffsetDateTime now) {
        var zone=ZoneId.of("Asia/Ho_Chi_Minh"); var today=now.atZoneSameInstant(zone).toLocalDate();
        if((start==null)!=(end==null)) throw BusinessException.invalid();
        if(start==null) { start=today.minusDays(6); end=today; }
        if(start.isAfter(end) || end.isAfter(today) || ChronoUnit.DAYS.between(start,end)+1>366) throw BusinessException.invalid();
        var endExclusive=end.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        var cutoff=endExclusive.isAfter(now)?now:endExclusive;
        return new Period(start.atStartOfDay(zone).toOffsetDateTime(),end,cutoff);
    }
    private Instant instant(Object value) {
        if(value instanceof Instant instant) return instant;
        return value instanceof OffsetDateTime t?t.toInstant():((java.sql.Timestamp)value).toInstant();
    }
    private Map<String,Object> breakdown(List<Object[]> rows) {
        var status=new TreeMap<String,Long>(); var severity=new TreeMap<String,Long>();
        rows.forEach(r->{status.merge(TechnicalIncidentService.STATUS_LABELS.get(r[3]),1L,Long::sum);severity.merge(TechnicalIncidentService.SEVERITY_LABELS.get(r[4]),1L,Long::sum);});
        return Map.of("status",status,"severity",severity);
    }
    public record Period(OffsetDateTime start,LocalDate endDate,OffsetDateTime cutoff) { }
}
