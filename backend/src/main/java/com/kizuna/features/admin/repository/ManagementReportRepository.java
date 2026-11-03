package com.kizuna.features.admin.repository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;
@Repository @RequiredArgsConstructor
public class ManagementReportRepository {
    private final EntityManager em;
    public List<Object[]> accounts(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT r.role_name,u.account_status::text,count(*),count(*) FILTER(WHERE u.created_at>=:start AND u.created_at<:end) FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.deleted_at IS NULL GROUP BY r.role_name,u.account_status ORDER BY r.role_name,u.account_status")
            .setParameter("start",start).setParameter("end",end).getResultList();
    }
}
