package com.kizuna.features.progress.util;

import com.kizuna.features.progress.dto.SubmissionState;
import com.kizuna.features.progress.entity.AssignmentSubmission;

import java.time.OffsetDateTime;

/**
 * Quy tắc nộp bài / chấm điểm FE-07 (hàm thuần, đã thống nhất với người phụ trách):
 * - Còn hạn (hoặc không có hạn): nộp và nộp lại nhiều lần cho tới khi được chấm.
 * - Quá hạn: chỉ được nộp 1 lần nếu chưa nộp lần nào; không nộp lại.
 * - Gia sư chấm lần nộp mới nhất, 1 lần duy nhất, sau khi hết hạn (bài không có hạn: chấm lúc nào cũng được).
 */
public final class AssignmentRules {

    private AssignmentRules() {
    }

    public static boolean dueOver(OffsetDateTime dueDate, OffsetDateTime now) {
        return dueDate != null && now.isAfter(dueDate);
    }

    public static boolean onTime(OffsetDateTime dueDate, OffsetDateTime submittedAt) {
        return dueDate == null || submittedAt == null || !submittedAt.isAfter(dueDate);
    }

    public static boolean canSubmit(OffsetDateTime dueDate, OffsetDateTime now, int attempts, boolean graded,
                                    boolean readOnly) {
        if (graded || readOnly) return false;
        return !dueOver(dueDate, now) || attempts == 0;
    }

    public static boolean canGrade(OffsetDateTime dueDate, OffsetDateTime now, boolean hasSubmission, boolean graded,
                                   boolean readOnly) {
        if (graded || readOnly || !hasSubmission) return false;
        return dueDate == null || dueOver(dueDate, now);
    }

    public static SubmissionState state(OffsetDateTime dueDate, OffsetDateTime now, AssignmentSubmission latest) {
        if (latest == null) {
            return dueOver(dueDate, now) ? SubmissionState.OVERDUE_NOT_SUBMITTED : SubmissionState.NOT_SUBMITTED;
        }
        if (latest.isFinalized()) return SubmissionState.GRADED;
        return onTime(dueDate, latest.getSubmittedAt()) ? SubmissionState.ON_TIME : SubmissionState.LATE;
    }
}
