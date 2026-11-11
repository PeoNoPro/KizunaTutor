package com.kizuna.features.admin.dto;
import java.util.*;
public record IncidentCreateRequest(@jakarta.validation.constraints.NotBlank(message="Vui lòng nhập tiêu đề sự cố.") String subject,
    @jakarta.validation.constraints.NotBlank(message="Vui lòng nhập mô tả sự cố.") String description,
    @jakarta.validation.constraints.NotNull(message="Vui lòng chọn mức độ.") String severity,
    @jakarta.validation.constraints.NotEmpty(message="Vui lòng chọn thành phần bị ảnh hưởng.") Set<String> components, UUID recurrenceOf,
    /** Chỉ dùng khi Quản lý báo sự cố từ một yêu cầu hỗ trợ được chuyển lên cho mình. */ UUID supportTicketId) {
    @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String key,com.fasterxml.jackson.databind.JsonNode value) { throw new IllegalArgumentException("Trường không được phép thay đổi."); }
}
