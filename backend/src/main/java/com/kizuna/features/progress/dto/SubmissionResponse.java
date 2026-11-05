package com.kizuna.features.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Một lần nộp bài. Không chứa dữ liệu tệp. */
public record SubmissionResponse(
        UUID submissionId,
        int attemptNumber,
        String content,
        String fileName,
        String fileType,
        OffsetDateTime submittedAt,
        boolean onTime,
        BigDecimal grade,
        String tutorFeedback,
        boolean graded,
        OffsetDateTime gradedAt) {
}
