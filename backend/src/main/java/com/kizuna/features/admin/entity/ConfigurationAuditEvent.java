package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="configuration_audit_events") @Getter @Setter @NoArgsConstructor
public class ConfigurationAuditEvent {
    @Id @Column(name="event_id") private UUID eventId;
     @Column(name="revision_id") private UUID revisionId;
     @Column(name="before_revision_id") private UUID beforeRevisionId;
     @Column(name="actor_id") private UUID actorId;
     @Column(name="occurred_at") private OffsetDateTime occurredAt;
     @Column(name="reason") private String reason;
}
