package com.kizuna.features.ticket.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
@Entity @Table(name="ticket_audit_events") @Getter @Setter @NoArgsConstructor
public class TicketAuditEvent {
 @Id @Column(name="event_id") private UUID eventId;
  @Column(name="ticket_id") private UUID ticketId;
  @Column(name="actor_user_id") private UUID actorUserId;
  @Column(name="event_type") private String eventType;
  @Column(name="occurred_at") private OffsetDateTime occurredAt;
  @Column(name="ticket_version") private long ticketVersion;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(name="before_values") private String beforeValues;
 @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(name="after_values") private String afterValues;
  @Column(name="note") private String note;
}
