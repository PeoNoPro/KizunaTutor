package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.dto.PageResponse;

import com.kizuna.features.admin.dto.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.features.admin.repository.*;
import com.kizuna.features.admin.repository.SystemConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class BookingPolicyConfigurationService implements BookingPolicySnapshotProvider {
    public static final UUID STATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final BookingPolicyStateRepository states;
    private final BookingPolicyRevisionRepository revisions;
    private final ConfigurationAuditEventRepository audits;
    private final SystemConfigurationRepository legacy;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor commands;
    private final Clock operationsClock;

    @Transactional(readOnly=true, timeout=3)
    public Map<String,Object> read() {
        actors.requireAdmin();
        var state = states.findById(STATE_ID).orElse(null);
        if (state == null || !validLegacy()) return Map.of("editable",false,"message","Cấu hình chưa đầy đủ. Vui lòng liên hệ người phụ trách triển khai.","holdMinutes",15);
        var revision=revisions.findById(state.getCurrentRevisionId()).orElseThrow(BusinessException::missing);
        if(!consistentLegacy(revision)) return Map.of("editable",false,"message","Cấu hình cần được người phụ trách kiểm tra lại.","holdMinutes",15);
        return response(state,revision);
    }

    @Transactional(timeout=4)
    public Map<String,Object> update(BookingPolicyUpdateRequest request) {
        var actor = actors.requireAdmin();
        int duration = integerField(request.durationMinutes(),15,120,"Thời lượng học thử phải là số nguyên từ 15 đến 120 phút.");
        int advance = integerField(request.minAdvanceHours(),1,168,"Thời gian đặt trước phải là số nguyên từ 1 đến 168 giờ.");
        if(request.reason()==null || request.reason().isBlank() || request.reason().codePointCount(0,request.reason().length())>2000)
            throw new BusinessException(400,"Lý do thay đổi cần từ 1 đến 2.000 ký tự và không chỉ gồm khoảng trắng.");
        var state = states.lock(STATE_ID).orElseThrow(BusinessException::conflict);
        commands.lockAdmin(actor.userId());
        OperationsValidation.version(request.expectedVersion(),state.getVersion());
        if (!validLegacy()) throw BusinessException.conflict();
        var before = revisions.findById(state.getCurrentRevisionId()).orElseThrow(BusinessException::missing);
        if(!consistentLegacy(before)) throw BusinessException.conflict();
        var revision = new BookingPolicyRevision();
        revision.setRevisionId(UUID.randomUUID()); revision.setRevisionNumber(before.getRevisionNumber()+1);
        revision.setDurationMinutes(duration); revision.setMinAdvanceHours(advance);
        revision.setEffectiveAt(OffsetDateTime.now(operationsClock)); revision.setChangedBy(actor.userId());
        revision.setChangeReason(request.reason()); revision.setProvenance("ADMIN"); revisions.save(revision);
        writeLegacy("TRIAL_DEFAULT_DURATION_MINUTES",duration); writeLegacy("BOOKING_MIN_HOURS_IN_ADVANCE",advance);
        var audit = new ConfigurationAuditEvent(); audit.setEventId(UUID.randomUUID());
        audit.setRevisionId(revision.getRevisionId()); audit.setBeforeRevisionId(before.getRevisionId());
        audit.setActorId(actor.userId()); audit.setOccurredAt(revision.getEffectiveAt()); audit.setReason(request.reason()); audits.save(audit);
        state.setCurrentRevisionId(revision.getRevisionId()); states.saveAndFlush(state);
        return response(state,revision);
    }

    @Transactional(readOnly=true, timeout=3)
    public PageResponse<Map<String,Object>> history(int page,int size) {
        actors.requireAdmin(); OperationsValidation.page(page,size);
        var result=audits.findOrdered(PageRequest.of(page,size));
        var revisionIds=new HashSet<UUID>(); var userIds=new HashSet<UUID>();
        result.forEach(a->{revisionIds.add(a.getBeforeRevisionId());revisionIds.add(a.getRevisionId());userIds.add(a.getActorId());});
        var versions=new HashMap<UUID,BookingPolicyRevision>(); revisions.findAllById(revisionIds).forEach(r->versions.put(r.getRevisionId(),r));
        var names=actors.names(userIds);
        return PageResponse.of(result.map(a -> {
            var before = versions.get(a.getBeforeRevisionId()); var after = versions.get(a.getRevisionId());
            return Map.<String,Object>of("before",values(before),"after",values(after),"reason",a.getReason(),"occurredAt",a.getOccurredAt(),"actorDisplayName",names.get(a.getActorId()));
        }));
    }
    @Override @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Snapshot lockCurrent() {
        var state=states.lock(STATE_ID).orElseThrow(BusinessException::conflict);
        var r=revisions.findById(state.getCurrentRevisionId()).orElseThrow(BusinessException::conflict);
        if(!validLegacy() || !consistentLegacy(r)) throw BusinessException.conflict();
        return new Snapshot(r.getRevisionId(),r.getDurationMinutes(),r.getMinAdvanceHours());
    }

    public static int integer(JsonNode node,int min,int max) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt() || node.intValue()<min || node.intValue()>max) throw BusinessException.invalid();
        return node.intValue();
    }
    private int integerField(JsonNode node,int min,int max,String message) {
        try { return integer(node,min,max); } catch(BusinessException ex) { throw new BusinessException(400,message); }
    }

    private boolean validLegacy() {
        return validValue("TRIAL_DEFAULT_DURATION_MINUTES",15,120) && validValue("BOOKING_MIN_HOURS_IN_ADVANCE",1,168);
    }
    private boolean validValue(String key,int min,int max) {
        return legacy.findById(key).map(v -> {
            try { String s=v.getConfigValue(); if(s==null || !s.matches("[0-9]{1,3}")) return false;
                int n=Integer.parseInt(s); return n>=min && n<=max;
            } catch(NumberFormatException ex) { return false; }
        }).orElse(false);
    }
    private void writeLegacy(String key,int value) {
        var v=legacy.findById(key).orElseThrow(BusinessException::conflict);
        v.setConfigValue(Integer.toString(value)); v.setUpdatedAt(OffsetDateTime.now(operationsClock)); legacy.save(v);
    }
    private boolean consistentLegacy(BookingPolicyRevision r) {
        return legacy.findById("TRIAL_DEFAULT_DURATION_MINUTES").map(v->Integer.toString(r.getDurationMinutes()).equals(v.getConfigValue())).orElse(false)
            && legacy.findById("BOOKING_MIN_HOURS_IN_ADVANCE").map(v->Integer.toString(r.getMinAdvanceHours()).equals(v.getConfigValue())).orElse(false);
    }
    private Map<String,Object> values(BookingPolicyRevision r) {
        return Map.of("durationMinutes",r.getDurationMinutes(),"minAdvanceHours",r.getMinAdvanceHours(),"revision",r.getRevisionNumber());
    }
    private Map<String,Object> response(BookingPolicyState s,BookingPolicyRevision r) {
        var result=new LinkedHashMap<>(values(r)); result.put("version",s.getVersion()); result.put("effectiveAt",r.getEffectiveAt());
        result.put("holdMinutes",15); result.put("editable",true);
        result.put("definitions",List.of(Map.of("label","Thời lượng học thử mặc định","unit","phút","min",15,"max",120),Map.of("label","Thời gian tối thiểu đặt trước","unit","giờ","min",1,"max",168)));
        return result;
    }
    private String actorName(UUID id) { return actors.displayName(id); }
}
