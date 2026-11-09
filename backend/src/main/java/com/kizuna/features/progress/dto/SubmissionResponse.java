package com.kizuna.features.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bài nộp của học viên (mỗi học viên một bài nộp cho mỗi bài tập; sửa thay vì nộp lại).
 * attemptNumber giữ cho dữ liệu cũ (trước đây mỗi lần nộp lại là một dòng). Không chứa dữ liệu tệp.
 */
public record SubmissionResponse(
        UUID submissionId,
        int attemptNumber,
        String content,
        List<FileAttachmentResponse> files,
        OffsetDateTime submittedAt,
        boolean onTime,
        BigDecimal grade,
        String tutorFeedback,
        boolean graded,
        OffsetDateTime gradedAt) {
}
