package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.TechnicalIncident;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface TechnicalIncidentRepository extends JpaRepository<TechnicalIncident,UUID>, JpaSpecificationExecutor<TechnicalIncident> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from TechnicalIncident i where i.incidentId=:id")
    java.util.Optional<TechnicalIncident> lock(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
