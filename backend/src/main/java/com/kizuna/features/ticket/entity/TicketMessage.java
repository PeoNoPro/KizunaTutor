package com.kizuna.features.ticket.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
@Entity @Table(name="ticket_messages") @Getter @Setter @NoArgsConstructor
public class TicketMessage {
 @Id @Column(name="message_id") private UUID messageId;
  @Column(name="ticket_id") private UUID ticketId;
  @Column(name="sender_id") private UUID senderId;
  @Column(name="message_text") private String messageText;
  @Column(name="sent_at") private OffsetDateTime sentAt;
  @Column(name="sequence_no") private long sequenceNo;
}
