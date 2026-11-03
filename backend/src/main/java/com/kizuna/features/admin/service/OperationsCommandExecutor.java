package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.OperationsActor;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class OperationsCommandExecutor {
    private final EntityManager em;
    /** Hold user and role shared locks through mutation commit; blocks concurrent revocation. */
    public void lockAdmin(UUID userId) {
        var found = em.createNativeQuery("SELECT u.user_id FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.user_id=:id AND u.deleted_at IS NULL AND u.account_status::text='ACTIVE' AND r.role_name='ROLE_ADMIN' AND r.is_active=true FOR SHARE OF u,r")
                .setParameter("id",userId).getResultList();
        if (found.isEmpty()) throw new BusinessException(403,"Người thực hiện hoặc người nhận không còn quyền quản trị.");
    }
}
