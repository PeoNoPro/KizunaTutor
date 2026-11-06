package com.kizuna.features.ticket.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
@Entity @Table(name="ticket_attachments") @Getter @Setter @NoArgsConstructor
public class TicketAttachment {
 @Id @Column(name="attachment_id") private UUID attachmentId;
  @Column(name="ticket_id") private UUID ticketId;
  @Column(name="message_id") private UUID messageId;
  @Column(name="uploaded_by") private UUID uploadedBy;
  @Column(name="original_name") private String originalName;
  @Column(name="content_type") private String contentType;
  @Column(name="size_bytes") private long sizeBytes;
  @Column(name="created_at") private OffsetDateTime createdAt;
 @Basic(fetch=FetchType.LAZY) @Column(name="content") private byte[] content;
}
