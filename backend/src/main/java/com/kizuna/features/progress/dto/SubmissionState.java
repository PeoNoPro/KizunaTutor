package com.kizuna.features.progress.dto;

/** Trạng thái nộp bài hiển thị (mã nội bộ; giao diện ánh xạ sang nhãn tiếng Việt). */
public enum SubmissionState {
    NOT_SUBMITTED,
    ON_TIME,
    LATE,
    OVERDUE_NOT_SUBMITTED,
    GRADED
}
