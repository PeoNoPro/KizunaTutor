package com.kizuna.features.admin.dto;
import com.fasterxml.jackson.databind.JsonNode;
public record BookingPolicyUpdateRequest(@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion, JsonNode durationMinutes, JsonNode minAdvanceHours,
    @jakarta.validation.constraints.NotBlank(message="Vui lòng nhập lý do thay đổi cấu hình.") String reason) {
    @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String key,JsonNode value) { throw new IllegalArgumentException("Trường không được phép thay đổi."); }
}
