package com.kizuna.features.ticket.dto;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class SupportTicketRequests {
 private SupportTicketRequests(){}
 public record Create(@NotBlank String subject,@NotBlank String category,@NotBlank String message,UUID relatedBookingId,UUID relatedMembershipId,UUID relatedTransactionId){}
 public record Command(@NotNull @JsonDeserialize(using=TicketVersionDeserializer.class) Long expectedVersion,
   UUID assigneeId,UUID managerId,String priority,String reason,String handlingSummary,String decisionText,String resolutionText){}
 public record Message(@NotNull @JsonDeserialize(using=TicketVersionDeserializer.class) Long expectedVersion,@NotBlank String message){}
}
