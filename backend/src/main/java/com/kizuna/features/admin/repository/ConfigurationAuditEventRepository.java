package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.ConfigurationAuditEvent;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface ConfigurationAuditEventRepository extends JpaRepository<ConfigurationAuditEvent,UUID> {
    @Query("select a from ConfigurationAuditEvent a, BookingPolicyRevision r where a.revisionId=r.revisionId order by r.revisionNumber desc")
    org.springframework.data.domain.Page<ConfigurationAuditEvent> findOrdered(org.springframework.data.domain.Pageable p);
}
