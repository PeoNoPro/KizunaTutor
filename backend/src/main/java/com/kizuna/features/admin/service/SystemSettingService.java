package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** Cấu hình dùng chung. Chỉ có mục đã nối vào nơi sử dụng thật; hiện là giới hạn tệp đính kèm yêu cầu hỗ trợ. */
@Service @RequiredArgsConstructor
public class SystemSettingService {
    public static final String ATTACHMENT_KEY="SUPPORT_ATTACHMENT_MAX_MB";
    /** Bằng giới hạn multipart của server; tăng cao hơn phải sửa cấu hình triển khai. */
    public static final int ATTACHMENT_CEILING_MB=10;
    private final EntityManager em;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;
    private final Clock operationsClock;

    public record AttachmentLimitUpdate(
        @NotNull(message="Vui lòng nhập giới hạn.") @Min(value=1,message="Giới hạn tối thiểu là 1 MB.") @Max(value=ATTACHMENT_CEILING_MB,message="Giới hạn tối đa là 10 MB.") Integer maxMegabytes,
        @NotNull(message="Vui lòng tải lại dữ liệu.") Long expectedVersion,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }

    private int currentMegabytes() {
        var rows=em.createNativeQuery("SELECT config_value FROM system_configurations WHERE config_key=:key").setParameter("key",ATTACHMENT_KEY).getResultList();
        if(rows.isEmpty())return ATTACHMENT_CEILING_MB;
        try {
            int value=Integer.parseInt(rows.get(0).toString().trim());
            return value<1||value>ATTACHMENT_CEILING_MB?ATTACHMENT_CEILING_MB:value;
        } catch(NumberFormatException exception) {return ATTACHMENT_CEILING_MB;}
    }
    private long version() {
        return ((Number)em.createNativeQuery("SELECT count(*) FROM system_setting_audit_events WHERE setting_key=:key").setParameter("key",ATTACHMENT_KEY).getSingleResult()).longValue();
    }

    /** Dùng cho nơi kiểm tra tệp: không đọc được cấu hình thì giữ mức trần đã có từ trước. */
    @Transactional(readOnly=true,timeout=3)
    public long attachmentMaxBytes() {
        return currentMegabytes()*1_000_000L;
    }

    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> attachmentLimit() {
        actors.requireAdmin();
        return Map.of("maxMegabytes",currentMegabytes(),"ceilingMegabytes",ATTACHMENT_CEILING_MB,"version",version());
    }

    @Transactional(timeout=5)
    public Map<String,Object> updateAttachmentLimit(AttachmentLimitUpdate r) {
        var actor=actors.requireAdmin();locks.lockAdmin(actor.userId());
        em.createNativeQuery("SELECT pg_advisory_xact_lock(11380492)").getSingleResult();
        if(version()!=r.expectedVersion())throw new BusinessException(409,"Cấu hình đã được người khác thay đổi. Vui lòng tải lại trước khi lưu.");
        int before=currentMegabytes();
        if(before==r.maxMegabytes())throw new BusinessException(400,"Giá trị mới trùng với giá trị hiện hành.");
        var now=OffsetDateTime.now(operationsClock);
        em.createNativeQuery("INSERT INTO system_configurations(config_key,config_value,data_type,description,updated_at) VALUES (:key,:value,'NUMBER','Giới hạn mỗi tệp đính kèm yêu cầu hỗ trợ (MB)',:now) ON CONFLICT(config_key) DO UPDATE SET config_value=excluded.config_value,updated_at=excluded.updated_at")
            .setParameter("key",ATTACHMENT_KEY).setParameter("value",r.maxMegabytes().toString()).setParameter("now",now).executeUpdate();
        em.createNativeQuery("INSERT INTO system_setting_audit_events(event_id,setting_key,actor_user_id,before_value,after_value,reason,occurred_at) VALUES (:id,:key,:actor,:before,:after,:reason,:now)")
            .setParameter("id",UUID.randomUUID()).setParameter("key",ATTACHMENT_KEY).setParameter("actor",actor.userId()).setParameter("before",String.valueOf(before)).setParameter("after",r.maxMegabytes().toString()).setParameter("reason",r.reason().trim()).setParameter("now",now).executeUpdate();
        return attachmentLimit();
    }

    @Transactional(readOnly=true,timeout=3)
    @SuppressWarnings("unchecked")
    public Map<String,Object> history(int page,int size) {
        actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        var rows=(List<Object[]>)em.createNativeQuery("SELECT a.full_name,e.before_value,e.after_value,e.reason,e.occurred_at FROM system_setting_audit_events e JOIN users a ON a.user_id=e.actor_user_id WHERE e.setting_key=:key ORDER BY e.occurred_at DESC,e.event_id DESC").setParameter("key",ATTACHMENT_KEY).setFirstResult(offset).setMaxResults(size).getResultList();
        var items=rows.stream().map(r->Map.of("actorDisplayName",r[0],"before",r[1],"after",r[2],"reason",r[3],"occurredAt",r[4])).toList();
        return Map.of("items",items,"page",page,"size",size,"totalElements",version());
    }
}
