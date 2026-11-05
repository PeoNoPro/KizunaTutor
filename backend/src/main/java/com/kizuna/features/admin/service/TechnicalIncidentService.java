package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.dto.PageResponse;

import com.kizuna.features.admin.dto.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.features.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class TechnicalIncidentService {
    public static final Set<String> STATES=Set.of("NEW","IN_PROGRESS","RESOLVED","CLOSED");
    public static final Set<String> SEVERITIES=Set.of("LOW","NORMAL","HIGH","CRITICAL");
    public static final Map<String,String> STATUS_LABELS=Map.of("NEW","Mới ghi nhận","IN_PROGRESS","Đang xử lý","RESOLVED","Đã giải quyết","CLOSED","Đã đóng");
    public static final Map<String,String> SEVERITY_LABELS=Map.of("LOW","Thấp","NORMAL","Bình thường","HIGH","Cao","CRITICAL","Nghiêm trọng");
    public static final Map<String,String> ACTION_LABELS=Map.of("CREATE","Ghi nhận sự cố","CLAIM","Nhận xử lý","REASSIGN","Phân công lại","PROGRESS","Ghi diễn biến","RESOLVE","Giải quyết","RESUME","Tiếp tục xử lý","CLOSE","Đóng sự cố","RELEASE","Trả lại danh sách chờ");
    private final TechnicalIncidentRepository incidents;
    private final TechnicalIncidentEventRepository events;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor commands;
    private final Clock operationsClock;

    @Transactional(timeout=4)
    public Map<String,Object> create(IncidentCreateRequest r) {
        var actor=actors.requireAdmin(); commands.lockAdmin(actor.userId());
        OperationsValidation.text(r.subject(),200); OperationsValidation.text(r.description(),10000);
        OperationsValidation.member(r.severity(),SEVERITIES);
        if(r.components()==null || r.components().isEmpty() || !OperationsComponentCatalog.CODES.containsAll(r.components())) throw BusinessException.invalid();
        if(r.recurrenceOf()!=null && !"CLOSED".equals(incidents.findById(r.recurrenceOf()).orElseThrow(BusinessException::missing).getStatus())) throw BusinessException.invalid();
        var i=new TechnicalIncident(); i.setIncidentId(UUID.randomUUID()); i.setSubject(r.subject()); i.setDescription(r.description());
        i.setSeverity(r.severity()); i.setStatus("NEW"); i.setReporterId(actor.userId()); i.setRecurrenceOf(r.recurrenceOf());
        i.setComponents(new LinkedHashSet<>(r.components())); incidents.saveAndFlush(i);
        append(i,actor.userId(),"CREATE",null,r.description(),null,null);
        return projection(i,actor.userId());
    }

    @Transactional(timeout=4)
    public Map<String,Object> command(UUID id,String action,IncidentCommandRequest r) {
        var actor=actors.requireAdmin();
        var i=incidents.lock(id).orElseThrow(BusinessException::missing);
        commands.lockAdmin(actor.userId());
        // Hai người cùng nhận: người chậm chân cần biết lý do thay vì lỗi phiên bản chung.
        if("CLAIM".equals(action)&&i.getAssigneeId()!=null)throw new BusinessException(409,"Sự cố đã có người nhận xử lý.");
        OperationsValidation.version(r.expectedVersion(),i.getVersion());
        if("CLOSED".equals(i.getStatus())) throw BusinessException.conflict();
        Map<String,Object> before=snapshot(i);
        apply(i,actor.userId(),action,r);
        i.setUpdatedAt(OffsetDateTime.now(operationsClock));
        // A progress event still increments optimistic version even when status stays unchanged.
        incidents.saveAndFlush(i);
        append(i,actor.userId(),action,before,"RESOLVE".equals(action)?r.result():r.detail(),r.reason(),r.verification());
        return projection(i,actor.userId());
    }

    private static final Map<String,Set<String>> COMMAND_FIELDS=Map.of("CLAIM",Set.of(),"RELEASE",Set.of("reason"),"PROGRESS",Set.of("detail"),"RESOLVE",Set.of("result","verification"),"RESUME",Set.of("reason"),"CLOSE",Set.of());
    // Lịch sử không sửa được nên từ chối trường không thuộc thao tác thay vì ghi kèm.
    private void onlyCommandFields(String action,IncidentCommandRequest r) {
        var allowed=COMMAND_FIELDS.getOrDefault(action,Set.of());var given=new HashMap<String,Object>();
        given.put("assigneeId",r.assigneeId());given.put("reason",r.reason());given.put("detail",r.detail());given.put("result",r.result());given.put("verification",r.verification());
        given.forEach((field,value)->{if(value!=null&&!allowed.contains(field))throw BusinessException.invalid();});
    }
    private void apply(TechnicalIncident i,UUID actor,String action,IncidentCommandRequest r) {
        onlyCommandFields(action,r);
        switch(action) {
            case "CLAIM" -> { state(i,"NEW"); if(i.getAssigneeId()!=null) throw BusinessException.conflict(); i.setAssigneeId(actor); i.setStatus("IN_PROGRESS"); }
            case "RELEASE" -> {
                state(i,"IN_PROGRESS"); OperationsValidation.text(r.reason(),2000);
                // Không chọn người thay thế; Admin khác chỉ gỡ được khi người phụ trách không còn hoạt động để tránh phiếu kẹt.
                if(!actor.equals(i.getAssigneeId())&&assigneeActive(i.getAssigneeId()))throw new BusinessException(403,"Chỉ người đang phụ trách được trả lại sự cố này.");
                i.setAssigneeId(null); i.setStatus("NEW");
            }
            case "PROGRESS" -> { owner(i,actor); state(i,"IN_PROGRESS"); OperationsValidation.text(r.detail(),10000); }
            case "RESOLVE" -> { owner(i,actor); state(i,"IN_PROGRESS"); OperationsValidation.text(r.result(),10000); OperationsValidation.text(r.verification(),10000); i.setStatus("RESOLVED"); }
            case "RESUME" -> { owner(i,actor); state(i,"RESOLVED"); OperationsValidation.text(r.reason(),2000); i.setStatus("IN_PROGRESS"); }
            case "CLOSE" -> { owner(i,actor); state(i,"RESOLVED"); i.setStatus("CLOSED"); }
            default -> throw BusinessException.invalid();
        }
    }

    @Transactional(readOnly=true, timeout=3)
    public Map<String,Object> detail(UUID id) {
        var actor=actors.requireAdmin();
        return projection(incidents.findById(id).orElseThrow(BusinessException::missing),actor.userId());
    }

    @Transactional(readOnly=true, timeout=3)
    public PageResponse<Map<String,Object>> list(String status,String severity,String component,UUID assignee,LocalDate from,LocalDate to,int page,int size) {
        var actor=actors.requireAdmin(); OperationsValidation.page(page,size);
        var spec=filter(status,severity,component,assignee,from,to);
        var result=incidents.findAll(spec,PageRequest.of(page,size,LIST_ORDER));
        var ids=new HashSet<UUID>(); result.forEach(i->{ids.add(i.getReporterId()); if(i.getAssigneeId()!=null)ids.add(i.getAssigneeId());});
        var names=actors.names(ids);
        return PageResponse.of(result.map(i->projection(i,actor.userId(),names)));
    }

    static final Sort LIST_ORDER=Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("incidentId"));
    /** Danh sách và tệp xuất dùng chung một bộ lọc để tệp luôn khớp màn hình. */
    static Specification<TechnicalIncident> filter(String status,String severity,String component,UUID assignee,LocalDate from,LocalDate to) {
        if(status!=null) OperationsValidation.member(status,STATES);
        if(severity!=null) OperationsValidation.member(severity,SEVERITIES);
        if(component!=null) OperationsValidation.member(component,OperationsComponentCatalog.CODES);
        if(from!=null && to!=null && from.isAfter(to)) throw BusinessException.invalid();
        return (root,q,cb)-> {
            var predicates=new ArrayList<Predicate>();
            if(status!=null) predicates.add(cb.equal(root.get("status"),status));
            if(severity!=null) predicates.add(cb.equal(root.get("severity"),severity));
            if(component!=null) predicates.add(cb.isMember(component,root.get("components")));
            if(assignee!=null) predicates.add(cb.equal(root.get("assigneeId"),assignee));
            if(from!=null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime()));
            if(to!=null) predicates.add(cb.lessThan(root.get("createdAt"),to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly=true, timeout=3)
    public PageResponse<Map<String,Object>> history(UUID id,int page,int size) {
        actors.requireAdmin(); OperationsValidation.page(page,size);
        if(!incidents.existsById(id)) throw BusinessException.missing();
        var result=events.findByIncidentIdOrderBySequenceNoAsc(id,PageRequest.of(page,size));
        var ids=new HashSet<UUID>(); result.forEach(e->{ids.add(e.getActorId()); snapshotUser(e.getPreviousState(),ids);snapshotUser(e.getNewState(),ids);});
        var names=actors.names(ids);
        return PageResponse.of(result.map(e->{
            Map<String,Object> m=new LinkedHashMap<>(); m.put("sequenceNo",e.getSequenceNo()); m.put("occurredAt",e.getOccurredAt());
            m.put("actionLabel",ACTION_LABELS.get(e.getAction())); m.put("actorDisplayName",names.get(e.getActorId()));
            m.put("detail",e.getDetail()); m.put("reason",e.getReason()); m.put("verification",e.getVerificationText());
            m.put("before",displaySnapshot(e.getPreviousState(),names)); m.put("after",displaySnapshot(e.getNewState(),names)); return m;
        }));
    }

    private void append(TechnicalIncident i,UUID actor,String action,Map<String,Object> before,String detail,String reason,String verification) {
        var event=new TechnicalIncidentEvent(); event.setEventId(UUID.randomUUID()); event.setIncidentId(i.getIncidentId());
        event.setSequenceNo(events.lastSequence(i.getIncidentId())+1); event.setActorId(actor);
        event.setOccurredAt("CREATE".equals(action)?i.getCreatedAt():OffsetDateTime.now(operationsClock));
        event.setAction(action); event.setPreviousState(before); event.setNewState(snapshot(i));
        event.setDetail(detail); event.setReason(reason); event.setVerificationText(verification); events.saveAndFlush(event);
    }
    private Map<String,Object> snapshot(TechnicalIncident i) {
        var s=new LinkedHashMap<String,Object>(); s.put("status",i.getStatus()); s.put("severity",i.getSeverity());
        s.put("assigneeId",i.getAssigneeId()==null?null:i.getAssigneeId().toString()); s.put("components",new ArrayList<>(i.getComponents())); return s;
    }
    private void snapshotUser(Map<String,Object> snapshot,Set<UUID> ids) {
        if(snapshot!=null && snapshot.get("assigneeId")!=null)ids.add(UUID.fromString(snapshot.get("assigneeId").toString()));
    }
    private Map<String,Object> displaySnapshot(Map<String,Object> s,Map<UUID,String> names) {
        if(s==null) return Map.of();
        var m=new LinkedHashMap<String,Object>(); m.put("statusLabel",STATUS_LABELS.get(s.get("status"))); m.put("severityLabel",SEVERITY_LABELS.get(s.get("severity")));
        Object id=s.get("assigneeId"); m.put("assigneeLabel",id==null?"Chưa phân công":names.get(UUID.fromString(id.toString()))); return m;
    }
    private boolean assigneeActive(UUID assignee) {
        try { actors.activeAdmin(assignee); return true; } catch(BusinessException exception) { return exception.status()>=500; }
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> projection(TechnicalIncident i,UUID actor) {
        var ids=new HashSet<UUID>(); ids.add(i.getReporterId()); if(i.getAssigneeId()!=null) ids.add(i.getAssigneeId());
        var result=projection(i,actor,actors.names(ids));
        if("IN_PROGRESS".equals(i.getStatus())&&!actor.equals(i.getAssigneeId())&&!assigneeActive(i.getAssigneeId()))((List<String>)result.get("allowedActions")).add("release");
        return result;
    }
    private Map<String,Object> projection(TechnicalIncident i,UUID actor,Map<UUID,String> names) {
        var m=new LinkedHashMap<String,Object>(); m.put("incidentId",i.getIncidentId()); m.put("subject",i.getSubject()); m.put("description",i.getDescription());
        m.put("statusLabel",STATUS_LABELS.get(i.getStatus())); m.put("severityLabel",SEVERITY_LABELS.get(i.getSeverity()));
        m.put("components",i.getComponents().stream().sorted().map(OperationsComponentCatalog.LABELS::get).toList());
        m.put("assigneeLabel",i.getAssigneeId()==null?"Chưa phân công":names.get(i.getAssigneeId())); m.put("reporterLabel",names.get(i.getReporterId()));
        m.put("createdAt",i.getCreatedAt()); m.put("updatedAt",i.getUpdatedAt()); m.put("version",i.getVersion()); m.put("recurrenceOf",i.getRecurrenceOf());
        var actions=new ArrayList<String>();
        if(!"CLOSED".equals(i.getStatus())) {
            if(i.getAssigneeId()==null) actions.add("claim");
            if(actor.equals(i.getAssigneeId())) { if("IN_PROGRESS".equals(i.getStatus())) actions.addAll(List.of("progress","resolve","release")); if("RESOLVED".equals(i.getStatus())) actions.addAll(List.of("resume","close")); }
        }
        m.put("allowedActions",actions); return m;
    }
    private void state(TechnicalIncident i,String required) { if(!required.equals(i.getStatus())) throw BusinessException.conflict(); }
    private void owner(TechnicalIncident i,UUID actor) { if(!actor.equals(i.getAssigneeId())) throw new BusinessException(403,"Chỉ người đang phụ trách được thực hiện thao tác này."); }
}
