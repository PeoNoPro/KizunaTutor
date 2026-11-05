package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.AssignmentDetailResponse;
import com.kizuna.features.progress.dto.AssignmentRequest;
import com.kizuna.features.progress.dto.AssignmentSummaryResponse;
import com.kizuna.features.progress.dto.SubmissionState;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.impl.AssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssignmentServiceImplTest {

    private ClassroomService classrooms;
    private AssignmentRepository assignments;
    private AssignmentSubmissionRepository submissions;
    private AssignmentServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        assignments = mock(AssignmentRepository.class);
        submissions = mock(AssignmentSubmissionRepository.class);
        service = new AssignmentServiceImpl(classrooms, assignments, submissions, CLOCK);

        tutor = tutor();
        learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);

        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(assignments.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            if (a.getAssignmentId() == null) a.setAssignmentId(UUID.randomUUID());
            return a;
        });
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private Assignment stored(java.time.OffsetDateTime due) {
        Assignment a = assignment(space, due);
        when(assignments.findByAssignmentIdAndSpace_SpaceId(a.getAssignmentId(), space.getSpaceId()))
                .thenReturn(Optional.of(a));
        return a;
    }

    // ----- UC-66 Create -----

    @Test
    void createsAssignmentWithDefaults() {
        AssignmentSummaryResponse r = service.create(tutorActor, space.getSpaceId(),
                new AssignmentRequest("  Ngữ pháp buổi 5 ", "  Viết 10 câu ", NOW.plusDays(2), null));

        assertThat(r.title()).isEqualTo("Ngữ pháp buổi 5");
        assertThat(r.description()).isEqualTo("Viết 10 câu");
        assertThat(r.totalPoints()).isEqualByComparingTo("100");
        assertThat(r.canEdit()).isTrue();
        verify(assignments).save(argThat(a -> a.getSpace() == space));
    }

    @Test
    void createWithoutDeadlineAllowed() {
        assertThat(service.create(tutorActor, space.getSpaceId(), new AssignmentRequest("Bài", null, null, null))
                .dueDate()).isNull();
    }

    @Test
    void createRejectsInvalidInput() {
        UUID id = space.getSpaceId();
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest(" ", null, null, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest("a".repeat(151), null, null, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest("Bài", "a".repeat(5001), null, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest("Bài", null, NOW.minusMinutes(1), null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest("Bài", null, null, new BigDecimal("0"))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(tutorActor, id, new AssignmentRequest("Bài", null, null, new BigDecimal("1000"))), HttpStatus.BAD_REQUEST);
        verify(assignments, never()).save(any());
    }

    @Test
    void createBlockedWhenClassroomNotManageable() {
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.create(learnerActor, space.getSpaceId(), new AssignmentRequest("Bài", null, null, null)),
                HttpStatus.FORBIDDEN);
        verify(assignments, never()).save(any());
    }

    // ----- UC-63 View -----

    @Test
    void listShowsLatestStateForLearner() {
        Assignment a = assignment(space, NOW.minusDays(1));
        when(assignments.findBySpace_SpaceIdOrderByCreatedAtDesc(space.getSpaceId())).thenReturn(List.of(a));
        when(submissions.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(a.getAssignmentId(), learner.getLearnerId()))
                .thenReturn(List.of(submission(a, learner, 1, NOW.minusDays(2), false)));

        AssignmentSummaryResponse r = service.list(learnerActor, space.getSpaceId()).get(0);

        assertThat(r.submissionState()).isEqualTo(SubmissionState.ON_TIME);
        assertThat(r.canSubmit()).isFalse();   // quá hạn, đã nộp
        assertThat(r.canGrade()).isFalse();    // học viên không chấm
        assertThat(r.canEdit()).isFalse();
    }

    @Test
    void listForTutorAllowsGradingAfterDeadline() {
        Assignment a = assignment(space, NOW.minusDays(1));
        when(assignments.findBySpace_SpaceIdOrderByCreatedAtDesc(space.getSpaceId())).thenReturn(List.of(a));
        when(submissions.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(any(), any()))
                .thenReturn(List.of(submission(a, learner, 1, NOW.minusHours(1), false)));

        AssignmentSummaryResponse r = service.list(tutorActor, space.getSpaceId()).get(0);

        assertThat(r.submissionState()).isEqualTo(SubmissionState.LATE);
        assertThat(r.canGrade()).isTrue();
        assertThat(r.canSubmit()).isFalse();
    }

    @Test
    void listDeniedForOutsiders() {
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.list(outsider, space.getSpaceId()), HttpStatus.FORBIDDEN);
        verify(assignments, never()).findBySpace_SpaceIdOrderByCreatedAtDesc(any());
    }

    @Test
    void detailReturnsSubmissionsOfClassroomLearnerOnly() {
        Assignment a = stored(NOW.plusDays(1));
        when(submissions.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(a.getAssignmentId(), learner.getLearnerId()))
                .thenReturn(List.of(submission(a, learner, 1, NOW.minusHours(2), false),
                        submission(a, learner, 2, NOW.minusHours(1), false)));

        AssignmentDetailResponse d = service.detail(learnerActor, space.getSpaceId(), a.getAssignmentId());

        assertThat(d.submissions()).extracting(s -> s.attemptNumber()).containsExactly(1, 2);
        assertThat(d.assignment().canSubmit()).isTrue();
        verify(submissions).findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(a.getAssignmentId(), learner.getLearnerId());
    }

    @Test
    void detailNotFoundForAssignmentOfOtherClassroom() {
        when(assignments.findByAssignmentIdAndSpace_SpaceId(any(), eq(space.getSpaceId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.detail(tutorActor, space.getSpaceId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    // ----- UC-95 Update -----

    @Test
    void updateAllowedBeforeAnyGrade() {
        Assignment a = stored(NOW.minusDays(1));
        AssignmentSummaryResponse r = service.update(tutorActor, space.getSpaceId(), a.getAssignmentId(),
                new AssignmentRequest("Bài mới", "Mô tả", NOW.plusDays(3), new BigDecimal("10")));

        assertThat(r.title()).isEqualTo("Bài mới");
        assertThat(a.getDueDate()).isEqualTo(NOW.plusDays(3));
        assertThat(a.getTotalPoints()).isEqualByComparingTo("10");
    }

    @Test
    void updateKeepingPastDeadlineUnchangedIsAllowed() {
        Assignment a = stored(NOW.minusDays(1));
        service.update(tutorActor, space.getSpaceId(), a.getAssignmentId(),
                new AssignmentRequest("Đổi tên", null, NOW.minusDays(1), null));
        assertThat(a.getTitle()).isEqualTo("Đổi tên");
    }

    @Test
    void updateRejectsNewPastDeadline() {
        Assignment a = stored(NOW.plusDays(1));
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), a.getAssignmentId(),
                new AssignmentRequest("Bài", null, NOW.minusHours(1), null)), HttpStatus.BAD_REQUEST);
        assertThat(a.getDueDate()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    void updateBlockedAfterGrading() {
        Assignment a = stored(NOW.minusDays(1));
        when(submissions.existsByAssignment_AssignmentIdAndFinalizedTrue(a.getAssignmentId())).thenReturn(true);
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), a.getAssignmentId(),
                new AssignmentRequest("Bài", null, null, null)), HttpStatus.CONFLICT);
        verify(assignments, never()).save(any());
    }

    @Test
    void updateCanClearDeadline() {
        Assignment a = stored(NOW.plusDays(1));
        service.update(tutorActor, space.getSpaceId(), a.getAssignmentId(), new AssignmentRequest("Bài", null, null, null));
        assertThat(a.getDueDate()).isNull();
    }
}
