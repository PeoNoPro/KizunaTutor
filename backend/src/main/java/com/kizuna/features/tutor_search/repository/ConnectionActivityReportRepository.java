package com.kizuna.features.tutor_search.repository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
@Repository @RequiredArgsConstructor
public class ConnectionActivityReportRepository {
    private final EntityManager em;
    public List<Object[]> counts(OffsetDateTime start,OffsetDateTime end) {
        return em.createNativeQuery("SELECT status::text,count(*) FROM connection_requests WHERE created_at>=:start AND created_at<:end GROUP BY status ORDER BY status")
            .setParameter("start",start).setParameter("end",end).getResultList();
    }
}
