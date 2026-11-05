package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Mục lịch sử thẩm định cho cán bộ học vụ (UC-84). Có ghi chú nội bộ — không bao giờ dùng cho phía gia sư.
 */
@Getter
@Builder
@AllArgsConstructor
public class VerificationHistoryItemResponse {
    private String action;
    private String actionLabel;
    private String feedback;
    private String reasonLabel;
    private String internalNote;
    private String officerName;
    private OffsetDateTime createdAt;
}
