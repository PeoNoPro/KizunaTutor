package com.kizuna.features.progress.dto;

import java.util.List;

/** Chi tiết bài tập kèm các lần nộp của học viên (cũ → mới). */
public record AssignmentDetailResponse(AssignmentSummaryResponse assignment, List<SubmissionResponse> submissions) {
}
