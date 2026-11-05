package com.kizuna.features.ticket.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
@Entity @Table(name="ticket_escalations") @Getter @Setter @NoArgsConstructor
public class TicketEscalation {
 @Id @Column(name="escalation_id") private UUID escalationId;
  @Column(name="ticket_id") private UUID ticketId;
  @Column(name="referred_by") private UUID referredBy;
  @Column(name="current_manager_id") private UUID currentManagerId;
  @Column(name="reason") private String reason;
  @Column(name="handling_summary") private String handlingSummary;
  @Column(name="created_at") private OffsetDateTime createdAt;
  @Column(name="returned_at") private OffsetDateTime returnedAt;
  @Column(name="pending") private boolean pending;
  @Column(name="recipient_revision") private long recipientRevision;
  @Column(name="decision_text") private String decisionText;
  @Column(name="decision_by") private UUID decisionBy;
  @Column(name="decision_revision") private Long decisionRevision;
  @Column(name="decided_at") private OffsetDateTime decidedAt;
}
