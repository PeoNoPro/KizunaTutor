package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.entity.PolicyDocumentVersion;
import com.kizuna.features.admin.repository.PolicyDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class PolicyDocumentReadService {
    private final PolicyDocumentVersionRepository documents;
    private final OperationsActorResolver actors;
    private final Clock operationsClock;
    @Transactional(readOnly=true,timeout=3)
    public List<Map<String,Object>> list() {
        actors.requireReader(); return approved().stream().map(d->projection(d,false)).toList();
    }
    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> detail(String type) {
        actors.requireReader();
        return approved().stream().filter(d->d.getDocumentType().equals(type)).findFirst().map(d->projection(d,true)).orElseThrow(BusinessException::missing);
    }
    private List<PolicyDocumentVersion> approved() {
        var rows=documents.current(OffsetDateTime.now(operationsClock)); var seen=new HashSet<String>();
        for(var d:rows) if(!seen.add(d.getDocumentType())) throw new BusinessException(409,"Chưa xác định được bản chính sách hiện hành. Vui lòng thử lại sau.");
        return rows;
    }
    private Map<String,Object> projection(PolicyDocumentVersion d,boolean content) {
        var m=new LinkedHashMap<String,Object>(); m.put("type",d.getDocumentType()); m.put("title",d.getTitle());
        m.put("versionLabel",d.getVersionLabel()); m.put("effectiveAt",d.getEffectiveFrom()); if(content)m.put("content",d.getApprovedContent()); return m;
    }
}
