package com.kizuna.features.admin.dto;
import java.util.UUID;
public record IncidentCommandRequest(@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion, UUID assigneeId, String reason, String detail, String result, String verification) {
    @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String key,com.fasterxml.jackson.databind.JsonNode value) { throw new IllegalArgumentException("Trường không được phép thay đổi."); }
}
