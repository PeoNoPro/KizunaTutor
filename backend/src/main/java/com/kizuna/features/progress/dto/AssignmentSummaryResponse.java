package com.kizuna.features.progress.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record AssignmentSummaryResponse(
        UUID assignmentId,
        String title,
        String description,
        OffsetDateTime dueDate,
        BigDecimal totalPoints,
        OffsetDateTime createdAt,
        boolean dueOver,
        SubmissionState submissionState,
        int attemptCount,
        BigDecimal grade,
        boolean canSubmit,
        boolean canGrade,
        boolean canEdit) {
}
