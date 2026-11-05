package com.kizuna.features.verification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Hồ sơ xác minh của một gia sư. Dùng cho trang của gia sư (UC-22, UC-82, UC-83)
 * và trang chi tiết của cán bộ học vụ (UC-27; khi đó có thêm tutorId, email).
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TutorVerificationResponse {
    private UUID tutorId;
    private String email;
    private String status;
    private String statusLabel;
    private Boolean editable;
    private OffsetDateTime submittedAt;
    private OffsetDateTime verifiedAt;
    private VerificationProfileDto profile;
    private List<VerificationDocumentResponse> documents;
    private List<String> missingRequirements;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private LatestFeedbackDto latestFeedback;
    private String message;
}
