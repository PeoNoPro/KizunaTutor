package com.kizuna.features.ticket.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import com.kizuna.core.entity.BaseEntity;
@Entity @Table(name="support_tickets") @Getter @Setter @NoArgsConstructor
public class SupportTicket extends BaseEntity {
 @Id @Column(name="ticket_id") private UUID ticketId;
  @Column(name="user_id") private UUID userId;
  @Column(name="assigned_to") private UUID assignedTo;
  @Column(name="escalated_to") private UUID escalatedTo;
  @Column(name="is_escalated") private boolean escalated;
  @Column(name="escalation_notes") private String escalationNotes;
 @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class) @Column(name="category") private SupportTicketCategory category;
  @Column(name="subject") private String subject;
 @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class) @Column(name="priority") private SupportTicketPriority priority;
 @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class) @Column(name="status") private SupportTicketStatus status;
  @Column(name="related_booking_id") private UUID relatedBookingId;
  @Column(name="related_membership_id") private UUID relatedMembershipId;
  @Column(name="related_transaction_id") private UUID relatedTransactionId;
 @Version @Column(name="version") private long version;
}
