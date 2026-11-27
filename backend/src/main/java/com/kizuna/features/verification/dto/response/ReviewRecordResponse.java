package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một quyết định thẩm định trong trang "Lịch sử xét duyệt" của cán bộ học vụ.
 * Có ghi chú nội bộ — chỉ dùng cho phía học vụ.
 */
@Getter
@Builder
@AllArgsConstructor
public class ReviewRecordResponse {
    private UUID historyId;
    private UUID tutorId;
    private String tutorName;
    private String tutorEmail;
    private String tutorJlptLevel;
    private String action;
    private String actionLabel;
    private String feedback;
    private String reasonLabel;
    private String internalNote;
    private String officerName;
    private boolean mine;
    private OffsetDateTime createdAt;
}
