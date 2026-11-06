package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="technical_incident_events") @Getter @Setter @NoArgsConstructor
public class TechnicalIncidentEvent {
    @Id @Column(name="event_id") private UUID eventId;
     @Column(name="incident_id") private UUID incidentId;
     @Column(name="sequence_no") private Long sequenceNo;
     @Column(name="actor_id") private UUID actorId;
     @Column(name="occurred_at") private OffsetDateTime occurredAt;
     @Column(name="action") private String action;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(name="previous_state") private Map<String,Object> previousState;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(name="new_state") private Map<String,Object> newState;
     @Column(name="detail") private String detail;
     @Column(name="reason") private String reason;
     @Column(name="verification_text") private String verificationText;
}
