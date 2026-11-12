package com.kizuna.features.progress.util;

import com.kizuna.features.progress.dto.SubmissionState;
import com.kizuna.features.progress.entity.AssignmentSubmission;

import java.time.OffsetDateTime;

/**
 * Quy tắc nộp bài / chấm điểm FE-07 (hàm thuần, đã thống nhất với người phụ trách):
 * - Mỗi học viên có MỘT bài nộp cho mỗi bài tập (nội dung + nhiều tệp).
 * - Chưa nộp: được nộp khi còn hạn; quá hạn mà chưa nộp lần nào thì được nộp muộn 1 lần.
 * - Đã nộp: được SỬA bài nộp (thêm/xóa/thay tệp, sửa nội dung) khi còn hạn và chưa được chấm;
 *   bài không có hạn: sửa được tới khi được chấm. Quá hạn hoặc đã chấm thì khóa.
 * - Gia sư chấm bài nộp 1 lần duy nhất, sau khi hết hạn (bài không có hạn: chấm lúc nào cũng được).
 */
public final class AssignmentRules {

    private AssignmentRules() {
    }

    /** Bài đã tới thời gian mở chưa (opensAt null = mở ngay). */
    public static boolean isOpen(OffsetDateTime opensAt, OffsetDateTime now) {
        return opensAt == null || !now.isBefore(opensAt);
    }

    public static boolean dueOver(OffsetDateTime dueDate, OffsetDateTime now) {
        return dueDate != null && now.isAfter(dueDate);
    }

    public static boolean onTime(OffsetDateTime dueDate, OffsetDateTime submittedAt) {
        return dueDate == null || submittedAt == null || !submittedAt.isAfter(dueDate);
    }

    /** Nộp lần đầu: chưa có bài nộp (còn hạn hoặc nộp muộn 1 lần khi quá hạn). */
    public static boolean canSubmit(OffsetDateTime dueDate, OffsetDateTime now, int attempts, boolean graded,
                                    boolean readOnly) {
        return !graded && !readOnly && attempts == 0;
    }

    /** Sửa bài đã nộp: còn hạn (hoặc không có hạn) và chưa được chấm. */
    public static boolean canEditSubmission(OffsetDateTime dueDate, OffsetDateTime now, boolean hasSubmission,
                                            boolean graded, boolean readOnly) {
        return hasSubmission && !graded && !readOnly && !dueOver(dueDate, now);
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
