package com.kizuna.features.admin.service;
import com.kizuna.features.admin.repository.ManagementReportRepository;
import com.kizuna.features.tutor_search.service.ConnectionActivityReportService;
import com.kizuna.features.booking.service.BookingActivityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.math.*;
@Service @RequiredArgsConstructor
public class ManagementReportService {
    private final OperationsActorResolver actors;
    private final ManagementReportRepository reports;
    private final ConnectionActivityReportService connections;
    private final BookingActivityReportService bookings;
    private final Clock operationsClock;
    private static final Map<String,String> ROLES=Map.of("ROLE_ADMIN","Quản trị viên","ROLE_MANAGER","Quản lý vận hành","ROLE_ACADEMIC_OFFICER","Cán bộ học vụ","ROLE_CS_STAFF","Chăm sóc khách hàng","ROLE_LEARNER","Học viên","ROLE_TUTOR","Gia sư");
    private static final Map<String,String> STATES=Map.of("ACTIVE","Đang hoạt động","SUSPENDED","Tạm khóa","BANNED","Đã bị khóa","PENDING","Chờ kích hoạt","INACTIVE","Ngừng hoạt động");
    private static final Map<String,String> CONNECTIONS=Map.of("PENDING","Chờ phản hồi","ACCEPTED","Đã chấp nhận","DECLINED","Đã từ chối","REJECTED","Đã từ chối","CANCELLED","Đã hủy","EXPIRED","Hết hạn");
    private static final Map<String,String> BOOKINGS=Map.of("PENDING","Chờ xác nhận","CONFIRMED","Đã xác nhận","COMPLETED","Đã hoàn tất","CANCELLED","Đã hủy","RESCHEDULED","Đã đổi lịch");
    private List<Map<String,Object>> groups(Map<String,Long> values,Map<String,String> labels) {
        return values.entrySet().stream().map(e->Map.<String,Object>of("label",labels.getOrDefault(e.getKey(),"Trạng thái khác"),"count",e.getValue())).toList();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=4)
    public Map<String,Object> read(LocalDate start,LocalDate end) {
        actors.requireReporter();var generated=OffsetDateTime.now(operationsClock);var period=OperationsIncidentReportService.period(start,end,generated);
        var rows=reports.accounts(period.start(),period.cutoff());long total=0,created=0;
        var roleCounts=new LinkedHashMap<String,Long>();var statusCounts=new LinkedHashMap<String,Long>();
        for(var row:rows){long count=((Number)row[2]).longValue();total+=count;created+=((Number)row[3]).longValue();roleCounts.merge((String)row[0],count,Long::sum);statusCounts.merge((String)row[1],count,Long::sum);}
        var connectionCounts=connections.countsCreatedBetween(period.start(),period.cutoff());var bookingCounts=bookings.countsCreatedBetween(period.start(),period.cutoff());
        long requests=connectionCounts.values().stream().mapToLong(Long::longValue).sum();long accepted=connectionCounts.getOrDefault("ACCEPTED",0L);
        var data=new LinkedHashMap<String,Object>();data.put("startDate",period.start().toLocalDate());data.put("endDate",period.endDate());data.put("generatedAt",generated.toString());
        data.put("totalAccounts",total);data.put("newAccounts",created);data.put("accountsByRole",groups(roleCounts,ROLES));data.put("accountsByStatus",groups(statusCounts,STATES));
        data.put("connectionCount",requests);data.put("connections",groups(connectionCounts,CONNECTIONS));data.put("bookingCount",bookingCounts.values().stream().mapToLong(Long::longValue).sum());data.put("bookings",groups(bookingCounts,BOOKINGS));
        data.put("acceptanceRatePercent",requests==0?null:BigDecimal.valueOf(accepted).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(requests),1,RoundingMode.HALF_UP));return data;
    }
}
