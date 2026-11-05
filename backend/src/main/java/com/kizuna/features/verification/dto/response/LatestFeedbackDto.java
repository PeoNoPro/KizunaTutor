package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Phản hồi gần nhất của cán bộ học vụ dành cho gia sư (A-10). Không chứa ghi chú nội bộ (GB-02).
 */
@Getter
@Builder
@AllArgsConstructor
public class LatestFeedbackDto {
    private String kind;
    private String kindLabel;
    private String reasonLabel;
    private String content;
    private OffsetDateTime createdAt;
}
