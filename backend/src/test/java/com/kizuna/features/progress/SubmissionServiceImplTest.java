package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.progress.dto.GradeRequest;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.dto.SubmissionResponse;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.entity.AssignmentSubmissionStatus;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubmissionServiceImplTest {

    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};

    private ClassroomService classrooms;
    private AssignmentRepository assignments;
    private AssignmentSubmissionRepository submissions;
    private SubmissionServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;
    private List<AssignmentSubmission> history;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        assignments = mock(AssignmentRepository.class);
        submissions = mock(AssignmentSubmissionRepository.class);
        service = new SubmissionServiceImpl(classrooms, assignments, submissions, new MaterialFileValidator(), CLOCK);

        tutor = tutor();
        learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        history = new ArrayList<>();

        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(submissions.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(any(), eq(learner.getLearnerId())))
                .thenAnswer(inv -> history);
        when(submissions.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission s = inv.getArgument(0);
            if (s.getSubmissionId() == null) s.setSubmissionId(UUID.randomUUID());
            return s;
        });
    }

    private Assignment stored(OffsetDateTime due) {
        Assignment a = assignment(space, due);
        when(assignments.findByAssignmentIdAndSpace_SpaceId(a.getAssignmentId(), space.getSpaceId())).thenReturn(Optional.of(a));
        return a;
    }

    private AssignmentSubmission previous(Assignment a, int attempt, OffsetDateTime at, boolean graded) {
        AssignmentSubmission s = submission(a, learner, attempt, at, graded);
        history.add(s);
        when(submissions.findBySubmissionIdAndAssignment_AssignmentId(s.getSubmissionId(), a.getAssignmentId()))
                .thenReturn(Optional.of(s));
        return s;
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "C:\\bai lam/bai-1.PDF", "application/pdf", PDF);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private void verifyNothingSaved() {
        verify(submissions, never()).save(any());
        verify(submissions, never()).updateFileData(any(), any());
    }

    // ----- UC-64 Submit -----

    @Test
    void firstSubmissionWithTextAndFile() {
        Assignment a = stored(NOW.plusDays(1));

        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "  Đáp án  ", pdf());

        assertThat(r.attemptNumber()).isEqualTo(1);
        assertThat(r.content()).isEqualTo("Đáp án");
        assertThat(r.fileName()).isEqualTo("bai-1.pdf");
        assertThat(r.fileType()).isEqualTo("PDF");
        assertThat(r.onTime()).isTrue();
        assertThat(r.submittedAt()).isEqualTo(NOW);
        verify(submissions).save(argThat(s -> s.getStatus() == AssignmentSubmissionStatus.SUBMITTED && !s.isFinalized()
                && s.getLearner() == learner));
        verify(submissions).updateFileData(r.submissionId(), PDF);
    }

    @Test
    void textOnlySubmissionStoresNoFile() {
        Assignment a = stored(null);
        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "Đáp án", null);
        assertThat(r.fileName()).isNull();
        verify(submissions, never()).updateFileData(any(), any());
    }

    @Test
    void submissionRequiresTextOrValidFile() {
        Assignment a = stored(NOW.plusDays(1));
        UUID s = space.getSpaceId();
        assertStatus(() -> service.submit(learnerActor, s, a.getAssignmentId(), "   ", null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.submit(learnerActor, s, a.getAssignmentId(), null,
                new MockMultipartFile("file", "virus.pdf", "application/pdf", new byte[]{0x4D, 0x5A})), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.submit(learnerActor, s, a.getAssignmentId(), "a".repeat(5001), null), HttpStatus.BAD_REQUEST);
        verifyNothingSaved();
    }

    @Test
    void lateFirstSubmissionAcceptedAndMarkedLate() {
        Assignment a = stored(NOW.minusDays(1));
        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "Đáp án", null);
        assertThat(r.onTime()).isFalse();
        assertThat(r.attemptNumber()).isEqualTo(1);
    }

    @Test
    void tutorCannotSubmit() {
        Assignment a = stored(NOW.plusDays(1));
        assertStatus(() -> service.submit(tutorActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.FORBIDDEN);
        verifyNothingSaved();
    }

    @Test
    void readOnlyClassroomBlocksSubmission() {
        Assignment a = stored(NOW.plusDays(1));
        when(classrooms.isReadOnly(space)).thenReturn(true);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.CONFLICT);
        verifyNothingSaved();
    }

    // ----- UC-96 Resubmit -----

    @Test
    void resubmitBeforeDeadlineCreatesNextAttempt() {
        Assignment a = stored(NOW.plusDays(1));
        previous(a, 1, NOW.minusHours(3), false);
        previous(a, 2, NOW.minusHours(2), false);

        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "Bản sửa", null);

        assertThat(r.attemptNumber()).isEqualTo(3);
    }

    @Test
    void resubmitWithoutDeadlineAllowed() {
        Assignment a = stored(null);
        previous(a, 1, NOW.minusDays(5), false);
        assertThat(service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "Bản sửa", null).attemptNumber())
                .isEqualTo(2);
    }

    @Test
    void resubmitAfterDeadlineBlocked() {
        Assignment a = stored(NOW.minusDays(1));
        previous(a, 1, NOW.minusDays(2), false);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.CONFLICT);
        verifyNothingSaved();
    }

    @Test
    void submitAfterGradedBlocked() {
        Assignment a = stored(null);
        previous(a, 1, NOW.minusDays(2), true);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.CONFLICT);
        verifyNothingSaved();
    }

    // ----- UC-67 Grade -----

    @Test
    void gradeLatestAfterDeadline() {
        Assignment a = stored(NOW.minusDays(1));
        previous(a, 1, NOW.minusDays(3), false);
        AssignmentSubmission latest = previous(a, 2, NOW.minusDays(2), false);

        SubmissionResponse r = service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), latest.getSubmissionId(),
                new GradeRequest(new BigDecimal("85.555"), "  Tốt  "));

        assertThat(r.grade()).isEqualByComparingTo("85.56");
        assertThat(r.tutorFeedback()).isEqualTo("Tốt");
        assertThat(r.graded()).isTrue();
        assertThat(latest.getStatus()).isEqualTo(AssignmentSubmissionStatus.GRADED);
        assertThat(latest.getGradedAt()).isEqualTo(NOW);
    }

    @Test
    void gradeWithoutDeadlineAllowedAnytime() {
        Assignment a = stored(null);
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        assertThat(service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(new BigDecimal("100"), null)).graded()).isTrue();
    }

    @Test
    void gradeBeforeDeadlineBlocked() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.CONFLICT);
        verify(submissions, never()).save(any());
    }

    @Test
    void gradeOnlyOnce() {
        Assignment a = stored(NOW.minusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusDays(2), true);
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.CONFLICT);
    }

    @Test
    void gradeOlderAttemptBlocked() {
        Assignment a = stored(NOW.minusDays(1));
        AssignmentSubmission old = previous(a, 1, NOW.minusDays(3), false);
        previous(a, 2, NOW.minusDays(2), false);
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), old.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.CONFLICT);
    }

    @Test
    void gradeValueMustBeWithinRange() {
        Assignment a = stored(NOW.minusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusDays(2), false);
        UUID sp = space.getSpaceId();
        assertStatus(() -> service.grade(tutorActor, sp, a.getAssignmentId(), s.getSubmissionId(), new GradeRequest(null, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.grade(tutorActor, sp, a.getAssignmentId(), s.getSubmissionId(), new GradeRequest(new BigDecimal("-1"), null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.grade(tutorActor, sp, a.getAssignmentId(), s.getSubmissionId(), new GradeRequest(new BigDecimal("100.01"), null)), HttpStatus.BAD_REQUEST);
        assertThat(s.isFinalized()).isFalse();
    }

    @Test
    void learnerCannotGrade() {
        Assignment a = stored(NOW.minusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusDays(2), false);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.grade(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.FORBIDDEN);
    }

    @Test
    void gradeUnknownSubmissionNotFound() {
        Assignment a = stored(NOW.minusDays(1));
        when(submissions.findBySubmissionIdAndAssignment_AssignmentId(any(), eq(a.getAssignmentId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), UUID.randomUUID(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.NOT_FOUND);
    }

    // ----- File -----

    @Test
    void fileReturnedForClassroomMember() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        s.setAttachmentUrl("bai-1.pdf");
        when(submissions.findFileData(s.getSubmissionId())).thenReturn(PDF);

        SubmissionFileContent f = service.getFile(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId());

        assertThat(f.data()).isEqualTo(PDF);
        assertThat(f.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void fileDeniedForOutsiderAndMissingWithoutAttachment() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.getFile(outsider, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId()), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.getFile(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId()), HttpStatus.NOT_FOUND);
        verify(submissions, never()).findFileData(any());
    }
}
