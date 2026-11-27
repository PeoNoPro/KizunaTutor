package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="policy_document_versions") @Getter @Setter @NoArgsConstructor
public class PolicyDocumentVersion {
    @Id @Column(name="document_version_id") private UUID documentVersionId;
     @Column(name="document_type") private String documentType;
     @Column(name="title") private String title;
     @Column(name="version_label") private String versionLabel;
     @Column(name="approved_content") private String approvedContent;
     @Column(name="publication_state") private String publicationState;
     @Column(name="effective_from") private OffsetDateTime effectiveFrom;
     @Column(name="effective_to") private OffsetDateTime effectiveTo;
     @Column(name="source_reference") private String sourceReference;
     @Column(name="approved_at") private OffsetDateTime approvedAt;
}
