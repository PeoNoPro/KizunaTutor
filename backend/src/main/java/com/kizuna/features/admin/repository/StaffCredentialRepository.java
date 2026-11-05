package com.kizuna.features.admin.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;

/** Hàng đợi email cấp tài khoản; không lưu mật khẩu. */
@Repository @RequiredArgsConstructor
public class StaffCredentialRepository {
    private final EntityManager em;
    public record Recipient(UUID userId,String email,String fullName) { }
    private static final String DELIVERABLE=" AND u.deleted_at IS NULL AND u.account_status::text='ACTIVE' AND u.must_change_password=true";

    public void enqueue(UUID user,UUID actor,OffsetDateTime now) {
        em.createNativeQuery("INSERT INTO staff_credential_emails(user_id,status,requested_by,attempts,updated_at) VALUES (:user,'PENDING',:actor,0,:now) ON CONFLICT(user_id) DO UPDATE SET status='PENDING',requested_by=excluded.requested_by,attempts=0,updated_at=excluded.updated_at,sent_at=NULL")
            .setParameter("user",user).setParameter("actor",actor).setParameter("now",now).executeUpdate();
    }
    @SuppressWarnings("unchecked")
    public Optional<Recipient> lockPending(UUID user) {
        var rows=(List<Object[]>)em.createNativeQuery("SELECT u.user_id,u.email,u.full_name FROM staff_credential_emails e JOIN users u ON u.user_id=e.user_id WHERE e.user_id=:user AND e.status='PENDING'"+DELIVERABLE+" FOR UPDATE OF e").setParameter("user",user).getResultList();
        return rows.stream().findFirst().map(r->new Recipient((UUID)r[0],(String)r[1],(String)r[2]));
    }
    public void issue(UUID user,String hash,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET password_hash=:hash WHERE user_id=:user").setParameter("hash",hash).setParameter("user",user).executeUpdate();
        em.createNativeQuery("UPDATE staff_credential_emails SET attempts=attempts+1,updated_at=:now WHERE user_id=:user").setParameter("now",now).setParameter("user",user).executeUpdate();
    }
    public void finish(UUID user,String status,OffsetDateTime now) {
        em.createNativeQuery("UPDATE staff_credential_emails SET status=:status,updated_at=:now,sent_at=CASE WHEN :status='SENT' THEN CAST(:now AS timestamptz) END WHERE user_id=:user")
            .setParameter("status",status).setParameter("now",now).setParameter("user",user).executeUpdate();
    }
    @SuppressWarnings("unchecked")
    public List<UUID> stalePending(OffsetDateTime before,int limit) {
        return em.createNativeQuery("SELECT e.user_id FROM staff_credential_emails e JOIN users u ON u.user_id=e.user_id WHERE e.status='PENDING' AND e.updated_at<:before"+DELIVERABLE+" ORDER BY e.updated_at").setParameter("before",before).setMaxResults(limit).getResultList();
    }
    @SuppressWarnings("unchecked")
    public Map<String,Long> counts() {
        var result=new HashMap<String,Long>(Map.of("PENDING",0L,"SENT",0L,"FAILED",0L));
        ((List<Object[]>)em.createNativeQuery("SELECT status,count(*) FROM staff_credential_emails GROUP BY status").getResultList()).forEach(r->result.put((String)r[0],((Number)r[1]).longValue()));
        return result;
    }
}
