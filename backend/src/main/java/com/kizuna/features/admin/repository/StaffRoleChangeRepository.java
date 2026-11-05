package com.kizuna.features.admin.repository;

import com.kizuna.core.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;

/** Đề xuất thăng chức; bản ghi đã chốt không còn được sửa. */
@Repository @RequiredArgsConstructor
public class StaffRoleChangeRepository {
    private final EntityManager em;
    public record Request(UUID id,UUID target,UUID requester,int fromRole,int toRole,String status,String reason) { }

    public Request lock(UUID id) {
        var rows=em.createNativeQuery("SELECT request_id,target_user_id,requester_user_id,from_role_id,to_role_id,status,reason FROM staff_role_change_requests WHERE request_id=:id FOR UPDATE").setParameter("id",id).getResultList();
        if(rows.isEmpty()) throw new BusinessException(404,"Không tìm thấy đề xuất thăng chức.");
        var r=(Object[])rows.get(0);
        return new Request((UUID)r[0],(UUID)r[1],(UUID)r[2],((Number)r[3]).intValue(),((Number)r[4]).intValue(),(String)r[5],(String)r[6]);
    }
    public boolean hasPending(UUID target) {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM staff_role_change_requests WHERE target_user_id=:target AND status='PENDING'").setParameter("target",target).getSingleResult()).longValue()>0;
    }
    @SuppressWarnings("unchecked")
    public Set<UUID> pendingTargets(Collection<UUID> targets) {
        if(targets.isEmpty()) return Set.of();
        return new HashSet<>(em.createNativeQuery("SELECT target_user_id FROM staff_role_change_requests WHERE status='PENDING' AND target_user_id IN (:targets)").setParameter("targets",targets).getResultList());
    }
    public void create(UUID id,UUID target,UUID requester,int from,int to,String reason,OffsetDateTime now) {
        em.createNativeQuery("INSERT INTO staff_role_change_requests(request_id,target_user_id,requester_user_id,from_role_id,to_role_id,reason,status,created_at) VALUES (:id,:target,:requester,:from,:to,:reason,'PENDING',:now)")
            .setParameter("id",id).setParameter("target",target).setParameter("requester",requester).setParameter("from",from).setParameter("to",to).setParameter("reason",reason).setParameter("now",now).executeUpdate();
    }
    public void decide(UUID id,String status,UUID actor,String note,OffsetDateTime now) {
        em.createNativeQuery("UPDATE staff_role_change_requests SET status=:status,decided_by=:actor,decision_note=CAST(:note AS varchar),decided_at=:now WHERE request_id=:id")
            .setParameter("id",id).setParameter("status",status).setParameter("actor",actor).setParameter("note",note).setParameter("now",now).executeUpdate();
    }
    private static String where(String status,UUID requester) {
        var parts=new ArrayList<String>();
        if(status!=null)parts.add("q.status=:status");
        if(requester!=null)parts.add("q.requester_user_id=:requester");
        return parts.isEmpty()?"":" WHERE "+String.join(" AND ",parts);
    }
    private static void bind(jakarta.persistence.Query query,String status,UUID requester) {
        if(status!=null)query.setParameter("status",status);
        if(requester!=null)query.setParameter("requester",requester);
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> list(String status,UUID requester,int offset,int size) {
        var query=em.createNativeQuery("SELECT q.request_id,t.full_name,t.email,a.full_name,f.role_name,n.role_name,q.reason,q.status,d.full_name,q.decision_note,q.created_at,q.decided_at,q.requester_user_id FROM staff_role_change_requests q JOIN users t ON t.user_id=q.target_user_id JOIN users a ON a.user_id=q.requester_user_id JOIN roles f ON f.role_id=q.from_role_id JOIN roles n ON n.role_id=q.to_role_id LEFT JOIN users d ON d.user_id=q.decided_by"+where(status,requester)+" ORDER BY q.created_at DESC,q.request_id");
        bind(query,status,requester);
        return query.setFirstResult(offset).setMaxResults(size).getResultList();
    }
    public long count(String status,UUID requester) {
        var query=em.createNativeQuery("SELECT count(*) FROM staff_role_change_requests q"+where(status,requester));
        bind(query,status,requester);
        return ((Number)query.getSingleResult()).longValue();
    }
}
