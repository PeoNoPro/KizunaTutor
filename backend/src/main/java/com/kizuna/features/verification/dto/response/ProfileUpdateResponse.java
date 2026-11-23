package com.kizuna.features.verification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Yêu cầu cập nhật hồ sơ gần nhất của gia sư đã được phê duyệt. */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileUpdateResponse {
    private UUID updateId;
    private String status;
    private String statusLabel;
    private String currentJlptLevel;
    private String requestedJlptLevel;
    private String note;
    private String feedback;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;
    private String reviewedByName;
    /** Giấy tờ đã xác minh gia sư đề nghị gỡ trong yêu cầu này (null nếu không có). */
    private List<RemovedDocument> removedDocuments;

    @Getter
    @AllArgsConstructor
    public static class RemovedDocument {
        private UUID id;
        private String title;
    }
}
