package com.kizuna.features.progress.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * FE-15: gợi ý chấm của AI cho một câu tự luận (chỉ gia sư của lớp xem được).
 * status: PENDING (AI đang chấm) | DONE (AI đã gợi ý) | FAILED (AI chưa chấm được); giao diện dùng statusLabel.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExerciseAiReviewResponse(UUID questionId, String status, String statusLabel, BigDecimal suggestedScore,
                                       String feedback, OffsetDateTime updatedAt) {
}
