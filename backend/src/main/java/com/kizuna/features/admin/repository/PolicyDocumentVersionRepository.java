package com.kizuna.features.admin.repository;
import com.kizuna.features.admin.entity.PolicyDocumentVersion;
import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface PolicyDocumentVersionRepository extends JpaRepository<PolicyDocumentVersion,UUID> {
    @org.springframework.data.jpa.repository.Query("select d from PolicyDocumentVersion d where d.publicationState='PUBLISHED' and d.effectiveFrom<=:now and (d.effectiveTo is null or d.effectiveTo>:now) order by d.documentType")
    java.util.List<PolicyDocumentVersion> current(@org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now);
}
