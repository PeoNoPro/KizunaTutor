package com.kizuna.features.progress.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bài tập trong danh sách.
 * attachments: các tệp đề bài (tối đa 10).
 * canSubmit: học viên chưa nộp và còn được nộp (còn hạn, hoặc quá hạn nhưng chưa nộp lần nào → nộp muộn 1 lần).
 * canEditSubmission: học viên đã nộp, còn hạn và chưa được chấm → được sửa bài nộp (thêm/xóa/thay tệp, sửa nội dung).
 */
@Builder
public record AssignmentSummaryResponse(
        UUID assignmentId,
        String title,
        String description,
        OffsetDateTime dueDate,
        BigDecimal totalPoints,
        List<FileAttachmentResponse> attachments,
        OffsetDateTime createdAt,
        boolean dueOver,
        SubmissionState submissionState,
        int attemptCount,
        BigDecimal grade,
        boolean canSubmit,
        boolean canEditSubmission,
        boolean canGrade,
        boolean canEdit) {
}
