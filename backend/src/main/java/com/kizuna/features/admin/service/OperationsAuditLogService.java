package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.repository.OperationsAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class OperationsAuditLogService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Map<String,String> SOURCES=Map.of("CONFIG","Cấu hình","STAFF","Nhân sự","INCIDENT","Sự cố","SHIFT","Lịch ca CS","NOTIFY","Thông báo");
    /** FE-12: Admin/Quản lý gửi thông báo (gồm gửi khẩn toàn hệ thống) và hủy thông báo hẹn giờ. */
    private static final Map<String,String> NOTIFY_ACTIONS=Map.of("SEND","Gửi thông báo","SCHEDULE","Hẹn giờ gửi thông báo","URGENT","Gửi thông báo khẩn toàn hệ thống","CANCEL","Hủy thông báo hẹn giờ");
    private static final Map<String,String> SHIFT_ACTIONS=Map.of("ASSIGN","Xếp ca","REMOVE","Gỡ ca");
    private final OperationsAuditLogRepository logs;
    private final OperationsActorResolver actors;
    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> list(String source,UUID actor,LocalDate from,LocalDate to,int page,int size) {
        actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        if(source!=null)OperationsValidation.member(source,SOURCES.keySet());
        if(from!=null&&to!=null&&from.isAfter(to))throw BusinessException.invalid();
        var filter=new OperationsAuditLogRepository.Filter(source,actor,from==null?null:from.atStartOfDay(ZONE).toOffsetDateTime(),to==null?null:to.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());
        return Map.of("items",logs.list(filter,offset,size).stream().map(this::view).toList(),"page",page,"size",size,"totalElements",logs.count(filter));
    }
    private Map<String,Object> view(Object[] r) {
        String source=(String)r[0],action=(String)r[4];var item=new LinkedHashMap<String,Object>();
        item.put("eventId",r[1]);item.put("occurredAt",r[2]);item.put("actorDisplayName",r[3]);item.put("sourceLabel",SOURCES.get(source));
        item.put("actionLabel",switch(source){case "CONFIG"->"Cập nhật cấu hình";case "STAFF"->StaffAccountService.ACTION_LABELS.get(action);case "SHIFT"->SHIFT_ACTIONS.get(action);case "NOTIFY"->NOTIFY_ACTIONS.get(action);default->TechnicalIncidentService.ACTION_LABELS.get(action);});
        item.put("targetLabel",r[5]);item.put("reason",r[6]);
        item.put("resultLabel",switch(source){case "CONFIG","NOTIFY"->(String)r[7];case "STAFF","SHIFT"->"Đã lưu";default->TechnicalIncidentService.STATUS_LABELS.get((String)r[8]);});
        item.put("incidentId",r[9]);return item;
    }
}
