package com.kizuna.features.admin.repository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
/** Hợp nhất ba nguồn lịch sử chỉ đọc; chỉ chọn cột đã làm sạch, không lấy nội dung điều tra hay giá trị bí mật. */
@Repository @RequiredArgsConstructor
public class OperationsAuditLogRepository {
    private static final String EVENTS="""
        SELECT 'CONFIG' AS source,e.event_id,e.occurred_at,e.actor_id,a.full_name AS actor_name,'UPDATE' AS action,'Cấu hình đặt học thử' AS target,e.reason,
               'Thời lượng '||b.duration_minutes||' → '||r.duration_minutes||' phút, đặt trước '||b.min_advance_hours||' → '||r.min_advance_hours||' giờ' AS result,NULL::text AS state,NULL::uuid AS incident_id
        FROM configuration_audit_events e JOIN users a ON a.user_id=e.actor_id
        JOIN booking_policy_revisions b ON b.revision_id=e.before_revision_id JOIN booking_policy_revisions r ON r.revision_id=e.revision_id
        UNION ALL
        SELECT 'STAFF',e.event_id,e.occurred_at,e.actor_user_id,a.full_name,e.action,t.full_name,e.reason,NULL,NULL,NULL
        FROM staff_account_audit_events e JOIN users a ON a.user_id=e.actor_user_id JOIN users t ON t.user_id=e.target_user_id
        UNION ALL
        SELECT 'INCIDENT',e.event_id,e.occurred_at,e.actor_id,a.full_name,e.action,i.subject,e.reason,NULL,e.new_state->>'status',i.incident_id
        FROM technical_incident_events e JOIN users a ON a.user_id=e.actor_id JOIN technical_incidents i ON i.incident_id=e.incident_id
        UNION ALL
        SELECT 'CONFIG',e.event_id,e.occurred_at,e.actor_user_id,a.full_name,'UPDATE','Giới hạn tệp đính kèm hỗ trợ',e.reason,e.before_value||' → '||e.after_value||' MB',NULL,NULL
        FROM system_setting_audit_events e JOIN users a ON a.user_id=e.actor_user_id
        UNION ALL
        SELECT 'STAFF',q.request_id,q.created_at,q.requester_user_id,a.full_name,'PROPOSE',t.full_name,q.reason,NULL,NULL,NULL
        FROM staff_role_change_requests q JOIN users a ON a.user_id=q.requester_user_id JOIN users t ON t.user_id=q.target_user_id
        UNION ALL
        SELECT 'STAFF',q.request_id,q.decided_at,q.decided_by,a.full_name,CASE q.status WHEN 'REJECTED' THEN 'REJECT' ELSE 'CANCEL' END,t.full_name,coalesce(q.decision_note,q.reason),NULL,NULL,NULL
        FROM staff_role_change_requests q JOIN users a ON a.user_id=q.decided_by JOIN users t ON t.user_id=q.target_user_id WHERE q.status IN ('REJECTED','CANCELLED')
        """;
    private final EntityManager em;
    public record Filter(String source,UUID actor,OffsetDateTime from,OffsetDateTime toExclusive) {
        String where() {
            var parts=new ArrayList<String>();
            if(source!=null)parts.add("source=:source");
            if(actor!=null)parts.add("actor_id=:actor");
            if(from!=null)parts.add("occurred_at>=:from");
            if(toExclusive!=null)parts.add("occurred_at<:to");
            return parts.isEmpty()?"":" WHERE "+String.join(" AND ",parts);
        }
        void bind(jakarta.persistence.Query query) {
            if(source!=null)query.setParameter("source",source);
            if(actor!=null)query.setParameter("actor",actor);
            if(from!=null)query.setParameter("from",from);
            if(toExclusive!=null)query.setParameter("to",toExclusive);
        }
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> list(Filter filter,int offset,int size) {
        var query=em.createNativeQuery("SELECT source,event_id,occurred_at,actor_name,action,target,reason,result,state,incident_id FROM ("+EVENTS+") log"+filter.where()+" ORDER BY occurred_at DESC,event_id DESC");
        filter.bind(query);return query.setFirstResult(offset).setMaxResults(size).getResultList();
    }
    public long count(Filter filter) {
        var query=em.createNativeQuery("SELECT count(*) FROM ("+EVENTS+") log"+filter.where());
        filter.bind(query);return ((Number)query.getSingleResult()).longValue();
    }
}
