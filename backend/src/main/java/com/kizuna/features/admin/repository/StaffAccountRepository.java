package com.kizuna.features.admin.repository;

import com.kizuna.core.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;

@Repository @RequiredArgsConstructor
public class StaffAccountRepository {
    private final EntityManager em;
    private static final String INTERNAL = "r.role_name IN ('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACADEMIC_OFFICER','ROLE_CS_STAFF')";
    private static final String SELECT = "SELECT u.user_id,u.full_name,u.email,u.phone,u.account_status::text,u.updated_at,r.role_id,r.role_name,r.is_active,u.must_change_password,e.status,e.sent_at FROM users u JOIN roles r ON r.role_id=u.role_id LEFT JOIN staff_credential_emails e ON e.user_id=u.user_id WHERE u.deleted_at IS NULL AND " + INTERNAL;

    public record Account(UUID id,String name,String email,String phone,String status,OffsetDateTime updatedAt,int roleId,String role,boolean roleActive,boolean mustChangePassword,String emailStatus,Object emailSentAt) { }
    private Account account(Object[] r) {
        var time = r[5] instanceof OffsetDateTime t ? t : r[5] instanceof Instant i ? i.atOffset(ZoneOffset.UTC) : ((java.sql.Timestamp)r[5]).toInstant().atOffset(ZoneOffset.UTC);
        return new Account((UUID)r[0],(String)r[1],(String)r[2],(String)r[3],(String)r[4],time,((Number)r[6]).intValue(),(String)r[7],Boolean.TRUE.equals(r[8]),Boolean.TRUE.equals(r[9]),(String)r[10],r[11]);
    }
    public List<Account> list(String query,int offset,int size) {
        String pattern="%"+query.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        var rows=em.createNativeQuery(SELECT+" AND (lower(u.full_name) LIKE :q OR lower(u.email) LIKE :q) ORDER BY u.created_at DESC,u.user_id")
            .setParameter("q",pattern).setFirstResult(offset).setMaxResults(size).getResultList();
        return ((List<Object[]>)rows).stream().map(this::account).toList();
    }
    public long count(String query) {
        String pattern="%"+query.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        return ((Number)em.createNativeQuery("SELECT count(*) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL AND "+INTERNAL+" AND (lower(u.full_name) LIKE :q OR lower(u.email) LIKE :q)")
            .setParameter("q",pattern).getSingleResult()).longValue();
    }
    private static final String MOVABLE=" AND r.role_name IN ('ROLE_ACADEMIC_OFFICER','ROLE_CS_STAFF') AND r.is_active=true AND u.account_status::text='ACTIVE' AND (lower(u.full_name) LIKE :q OR lower(u.email) LIKE :q)";
    public List<Account> listMovable(String query,int offset,int size) {
        String pattern="%"+query.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        var rows=em.createNativeQuery(SELECT+MOVABLE+" ORDER BY u.full_name,u.user_id").setParameter("q",pattern).setFirstResult(offset).setMaxResults(size).getResultList();
        return ((List<Object[]>)rows).stream().map(this::account).toList();
    }
    public long countMovable(String query) {
        String pattern="%"+query.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        return ((Number)em.createNativeQuery("SELECT count(*) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL"+MOVABLE).setParameter("q",pattern).getSingleResult()).longValue();
    }
    public int activeRoleId(String name) {
        var rows=em.createNativeQuery("SELECT r.role_id FROM roles r WHERE r.role_name=:name AND r.is_active=true FOR SHARE OF r").setParameter("name",name).getResultList();
        if(rows.isEmpty()) throw new BusinessException(400,"Vai trò đích đang ngừng sử dụng.");
        return ((Number)rows.get(0)).intValue();
    }
    public Account get(UUID id,boolean lock) {
        var rows=em.createNativeQuery(SELECT+" AND u.user_id=:id"+(lock?" FOR UPDATE OF u":"")).setParameter("id",id).getResultList();
        if(rows.isEmpty()) throw new BusinessException(404,"Không tìm thấy tài khoản nhân viên.");
        return account((Object[])rows.get(0));
    }
    public void serializeWrites() { em.createNativeQuery("SELECT pg_advisory_xact_lock(11380491)").getSingleResult(); }
    public boolean emailExists(String email,UUID except) {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM users WHERE lower(email)=:email AND user_id<>:except")
            .setParameter("email",email).setParameter("except",except).getSingleResult()).longValue()>0;
    }
    @SuppressWarnings("unchecked")
    public Set<String> existingEmails(Collection<String> emails) {
        if(emails.isEmpty()) return Set.of();
        return new HashSet<>(em.createNativeQuery("SELECT lower(email) FROM users WHERE lower(email) IN (:emails)").setParameter("emails",emails).getResultList());
    }
    /** Email thuộc nhân viên đang bị khóa, để Admin biết nên mở lại thay vì tạo mới. */
    @SuppressWarnings("unchecked")
    public Set<String> lockedStaffEmails(Collection<String> emails) {
        if(emails.isEmpty()) return Set.of();
        return new HashSet<>(em.createNativeQuery("SELECT lower(u.email) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL AND "+INTERNAL+" AND u.account_status::text IN ('SUSPENDED','BANNED') AND lower(u.email) IN (:emails)").setParameter("emails",emails).getResultList());
    }
    public String activeRole(int id) {
        var rows=em.createNativeQuery("SELECT r.role_name FROM roles r WHERE r.role_id=:id AND r.is_active=true AND "+INTERNAL+" FOR SHARE OF r").setParameter("id",id).getResultList();
        if(rows.isEmpty()) throw new BusinessException(400,"Vui lòng chọn vai trò nội bộ đang hoạt động.");
        return (String)rows.get(0);
    }
    public long activeAdmins() {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL AND u.account_status::text='ACTIVE' AND r.is_active=true AND r.role_name='ROLE_ADMIN'").getSingleResult()).longValue();
    }
    public void create(UUID id,String name,String email,String phone,int roleId,String hash,OffsetDateTime now) {
        em.createNativeQuery("INSERT INTO users(user_id,full_name,email,phone,role_id,password_hash,account_status,must_change_password,created_at,updated_at) VALUES (:id,:name,:email,:phone,:role,:hash,'ACTIVE',true,:now,:now)")
            .setParameter("id",id).setParameter("name",name).setParameter("email",email).setParameter("phone",phone).setParameter("role",roleId).setParameter("hash",hash).setParameter("now",now).executeUpdate();
    }
    public void update(UUID id,String name,String email,String phone,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET full_name=:name,email=:email,phone=:phone,updated_at=:now WHERE user_id=:id")
            .setParameter("id",id).setParameter("name",name).setParameter("email",email).setParameter("phone",phone).setParameter("now",now).executeUpdate();
    }
    public void resetPassword(UUID id,String hash,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET password_hash=:hash,must_change_password=true,updated_at=:now WHERE user_id=:id").setParameter("id",id).setParameter("hash",hash).setParameter("now",now).executeUpdate();
    }
    public void changeRole(UUID id,int roleId,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET role_id=:role,updated_at=:now WHERE user_id=:id").setParameter("role",roleId).setParameter("id",id).setParameter("now",now).executeUpdate();
    }
    public void changeStatus(UUID id,String status,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET account_status=CAST(:status AS user_account_status_enum),updated_at=:now WHERE user_id=:id")
            .setParameter("status",status).setParameter("id",id).setParameter("now",now).executeUpdate();
    }
    /** Xóa mềm: đánh dấu deleted_at và chuyển Tạm khóa để mọi đường đăng nhập/xác thực đều từ chối. */
    public void softDelete(UUID id,OffsetDateTime now) {
        em.createNativeQuery("UPDATE users SET deleted_at=:now,account_status=CAST('SUSPENDED' AS user_account_status_enum),updated_at=:now WHERE user_id=:id AND deleted_at IS NULL")
            .setParameter("id",id).setParameter("now",now).executeUpdate();
    }
    /** Gỡ ca làm CS từ ngày cho trước trở đi, ghi lịch sử gỡ ca (bảng sự kiện chỉ thêm). */
    public void removeUpcomingShifts(UUID staffId,UUID actor,java.time.LocalDate from) {
        em.createNativeQuery("WITH d AS (DELETE FROM support_shifts WHERE staff_id=:staff AND shift_date>=:from RETURNING shift_id,staff_id,shift_date,shift_code) "
            + "INSERT INTO support_shift_events(event_id,shift_id,staff_id,shift_date,shift_code,action,actor_id) SELECT gen_random_uuid(),shift_id,staff_id,shift_date,shift_code,'REMOVE',:actor FROM d")
            .setParameter("staff",staffId).setParameter("from",from).setParameter("actor",actor).executeUpdate();
    }
    public void audit(UUID actor,UUID target,String action,String reason,OffsetDateTime now) {
        em.createNativeQuery("INSERT INTO staff_account_audit_events(event_id,actor_user_id,target_user_id,action,reason,occurred_at) VALUES (:id,:actor,:target,:action,:reason,:now)")
            .setParameter("id",UUID.randomUUID()).setParameter("actor",actor).setParameter("target",target).setParameter("action",action).setParameter("reason",reason).setParameter("now",now).executeUpdate();
    }
    public List<Object[]> roles() {
        return em.createNativeQuery("SELECT r.role_id,r.role_name,r.is_active,count(u.user_id) FROM roles r LEFT JOIN users u ON u.role_id=r.role_id AND u.deleted_at IS NULL WHERE r.role_name IN ('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACADEMIC_OFFICER','ROLE_CS_STAFF','ROLE_LEARNER','ROLE_TUTOR') GROUP BY r.role_id,r.role_name,r.is_active ORDER BY r.role_id").getResultList();
    }
    public List<Object[]> history(int offset,int size) {
        return em.createNativeQuery("SELECT a.full_name,t.full_name,e.action,e.reason,e.occurred_at FROM staff_account_audit_events e JOIN users a ON a.user_id=e.actor_user_id JOIN users t ON t.user_id=e.target_user_id ORDER BY e.occurred_at DESC,e.event_id DESC")
            .setFirstResult(offset).setMaxResults(size).getResultList();
    }
    public long historyCount() { return ((Number)em.createNativeQuery("SELECT count(*) FROM staff_account_audit_events").getSingleResult()).longValue(); }
}
