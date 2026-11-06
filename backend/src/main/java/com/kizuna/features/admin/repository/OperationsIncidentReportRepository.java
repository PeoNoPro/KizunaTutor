package com.kizuna.features.admin.repository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
@Repository @RequiredArgsConstructor
public class OperationsIncidentReportRepository {
    private final EntityManager em;
    @SuppressWarnings("unchecked")
    public List<Object[]> asOf(OffsetDateTime start,OffsetDateTime cutoff) {
        return em.createNativeQuery("""
            SELECT i.incident_id,i.subject,i.created_at,
                   e.new_state->>'status',e.new_state->>'severity',
                   EXISTS(SELECT 1 FROM technical_incident_events r WHERE r.incident_id=i.incident_id AND r.action='RESOLVE' AND r.occurred_at>=:start AND r.occurred_at<:cutoff)
            FROM technical_incidents i
            JOIN LATERAL (SELECT new_state FROM technical_incident_events x
              WHERE x.incident_id=i.incident_id AND x.occurred_at<:cutoff ORDER BY x.sequence_no DESC LIMIT 1) e ON true
            WHERE i.created_at<:cutoff ORDER BY i.created_at DESC,i.incident_id DESC
            """).setParameter("start",start).setParameter("cutoff",cutoff).getResultList();
    }
}
