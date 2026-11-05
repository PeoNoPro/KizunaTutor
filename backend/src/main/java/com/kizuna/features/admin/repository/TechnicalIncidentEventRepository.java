package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.TechnicalIncidentEvent;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface TechnicalIncidentEventRepository extends JpaRepository<TechnicalIncidentEvent,UUID> {
    org.springframework.data.domain.Page<TechnicalIncidentEvent> findByIncidentIdOrderBySequenceNoAsc(java.util.UUID id,org.springframework.data.domain.Pageable p);
    @org.springframework.data.jpa.repository.Query("select coalesce(max(e.sequenceNo),0) from TechnicalIncidentEvent e where e.incidentId=:id")
    long lastSequence(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
    @org.springframework.data.jpa.repository.Query("select e from TechnicalIncidentEvent e where e.action='RESOLVE' and e.incidentId in :ids order by e.sequenceNo")
    java.util.List<TechnicalIncidentEvent> findResolutions(@org.springframework.data.repository.query.Param("ids") java.util.Collection<java.util.UUID> ids);
}
