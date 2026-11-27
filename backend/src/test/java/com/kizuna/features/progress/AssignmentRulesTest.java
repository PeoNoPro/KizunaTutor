package com.kizuna.features.progress;

import com.kizuna.features.progress.dto.SubmissionState;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.util.AssignmentRules;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static com.kizuna.features.progress.ProgressTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class AssignmentRulesTest {

    private static final OffsetDateTime FUTURE = NOW.plusDays(1);
    private static final OffsetDateTime PAST = NOW.minusDays(1);

    @Test
    void dueOverOnlyWhenDeadlinePassed() {
        assertThat(AssignmentRules.dueOver(null, NOW)).isFalse();
        assertThat(AssignmentRules.dueOver(FUTURE, NOW)).isFalse();
        assertThat(AssignmentRules.dueOver(NOW, NOW)).isFalse();
        assertThat(AssignmentRules.dueOver(PAST, NOW)).isTrue();
    }

    @Test
    void onTimeIncludesExactDeadline() {
        assertThat(AssignmentRules.onTime(NOW, NOW)).isTrue();
        assertThat(AssignmentRules.onTime(NOW, NOW.plusSeconds(1))).isFalse();
        assertThat(AssignmentRules.onTime(null, NOW)).isTrue();
    }

    @Test
    void beforeDeadlineCanSubmitManyTimesUntilGraded() {
        assertThat(AssignmentRules.canSubmit(FUTURE, NOW, 0, false, false)).isTrue();
        assertThat(AssignmentRules.canSubmit(FUTURE, NOW, 3, false, false)).isFalse(); // đã nộp → sửa, không nộp lại
        assertThat(AssignmentRules.canEditSubmission(FUTURE, NOW, true, false, false)).isTrue();
        assertThat(AssignmentRules.canEditSubmission(FUTURE, NOW, false, false, false)).isFalse();
        assertThat(AssignmentRules.canSubmit(FUTURE, NOW, 1, true, false)).isFalse();
    }

    @Test
    void afterDeadlineOnlyOneSubmissionIfNoneBefore() {
        assertThat(AssignmentRules.canSubmit(PAST, NOW, 0, false, false)).isTrue();
        assertThat(AssignmentRules.canSubmit(PAST, NOW, 1, false, false)).isFalse();
        assertThat(AssignmentRules.canEditSubmission(PAST, NOW, true, false, false)).isFalse(); // quá hạn khóa sửa
        assertThat(AssignmentRules.canSubmit(PAST, NOW, 0, true, false)).isFalse();
    }

    @Test
    void noDeadlineAllowsResubmissionUntilGraded() {
        assertThat(AssignmentRules.canSubmit(null, NOW, 0, false, false)).isTrue();
        assertThat(AssignmentRules.canEditSubmission(null, NOW, true, false, false)).isTrue();  // không hạn: sửa tới khi chấm
        assertThat(AssignmentRules.canEditSubmission(null, NOW, true, true, false)).isFalse();
        assertThat(AssignmentRules.canSubmit(null, NOW, 5, true, false)).isFalse();
    }

    @Test
    void readOnlyBlocksEverything() {
        assertThat(AssignmentRules.canSubmit(FUTURE, NOW, 0, false, true)).isFalse();
        assertThat(AssignmentRules.canEditSubmission(FUTURE, NOW, true, false, true)).isFalse();
        assertThat(AssignmentRules.canGrade(PAST, NOW, true, false, true)).isFalse();
    }

    @Test
    void gradeOnlyAfterDeadlineOnceWithSubmission() {
        assertThat(AssignmentRules.canGrade(FUTURE, NOW, true, false, false)).isFalse();
        assertThat(AssignmentRules.canGrade(PAST, NOW, true, false, false)).isTrue();
        assertThat(AssignmentRules.canGrade(PAST, NOW, false, false, false)).isFalse();
        assertThat(AssignmentRules.canGrade(PAST, NOW, true, true, false)).isFalse();
        assertThat(AssignmentRules.canGrade(null, NOW, true, false, false)).isTrue();
    }

    @Test
    void stateLabels() {
        assertThat(AssignmentRules.state(FUTURE, NOW, null)).isEqualTo(SubmissionState.NOT_SUBMITTED);
        assertThat(AssignmentRules.state(PAST, NOW, null)).isEqualTo(SubmissionState.OVERDUE_NOT_SUBMITTED);
        AssignmentSubmission onTime = AssignmentSubmission.builder().submittedAt(PAST.minusHours(1)).build();
        AssignmentSubmission late = AssignmentSubmission.builder().submittedAt(PAST.plusHours(1)).build();
        AssignmentSubmission graded = AssignmentSubmission.builder().submittedAt(PAST.plusHours(1)).finalized(true).build();
        assertThat(AssignmentRules.state(PAST, NOW, onTime)).isEqualTo(SubmissionState.ON_TIME);
        assertThat(AssignmentRules.state(PAST, NOW, late)).isEqualTo(SubmissionState.LATE);
        assertThat(AssignmentRules.state(PAST, NOW, graded)).isEqualTo(SubmissionState.GRADED);
    }
}
