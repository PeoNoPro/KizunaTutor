package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="booking_policy_revisions") @Getter @Setter @NoArgsConstructor
public class BookingPolicyRevision {
    @Id @Column(name="revision_id") private UUID revisionId;
     @Column(name="revision_number") private Long revisionNumber;
     @Column(name="duration_minutes") private Integer durationMinutes;
     @Column(name="min_advance_hours") private Integer minAdvanceHours;
     @Column(name="effective_at") private OffsetDateTime effectiveAt;
     @Column(name="changed_by") private UUID changedBy;
     @Column(name="change_reason") private String changeReason;
     @Column(name="provenance") private String provenance;
}
