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
import com.kizuna.features.progress.entity.SubmissionFile;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.repository.SubmissionFileRepository;
import com.kizuna.features.progress.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final byte[] DOCX = {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};

    private ClassroomService classrooms;
    private AssignmentRepository assignments;
    private AssignmentSubmissionRepository submissions;
    private SubmissionFileRepository files;
    private SubmissionServiceImpl service;

    private LearningSpace space;
    private Learner learner;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;
    private List<AssignmentSubmission> history;
    /** Tệp đang lưu (giả lập bảng submission_files). */
    private List<SubmissionFile> storedFiles;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        assignments = mock(AssignmentRepository.class);
        submissions = mock(AssignmentSubmissionRepository.class);
        files = mock(SubmissionFileRepository.class);
        service = new SubmissionServiceImpl(classrooms, assignments, submissions, files, new MaterialFileValidator(), CLOCK);

        Tutor tutor = tutor();
        learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        history = new ArrayList<>();
        storedFiles = new ArrayList<>();

        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(submissions.findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(any(), eq(learner.getLearnerId())))
                .thenAnswer(inv -> history);
        when(submissions.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission s = inv.getArgument(0);
            if (s.getSubmissionId() == null) s.setSubmissionId(UUID.randomUUID());
            return s;
        });
        when(files.insertFile(any(), any(), anyString(), anyLong(), any())).thenAnswer(inv -> {
            storedFiles.add(SubmissionFile.builder().fileId(inv.getArgument(0)).submissionId(inv.getArgument(1))
                    .fileName(inv.getArgument(2)).fileSize(inv.getArgument(3)).build());
            return 1;
        });
        when(files.findBySubmissionIdOrderByCreatedAtAscFileNameAsc(any())).thenAnswer(inv ->
                storedFiles.stream().filter(f -> f.getSubmissionId().equals(inv.getArgument(0))).toList());
        doAnswer(inv -> {
            Iterable<SubmissionFile> gone = inv.getArgument(0);
            gone.forEach(storedFiles::remove);
            return null;
        }).when(files).deleteAll(anyIterable());
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

    private SubmissionFile storedFile(AssignmentSubmission s, String name) {
        SubmissionFile f = SubmissionFile.builder().fileId(UUID.randomUUID()).submissionId(s.getSubmissionId())
                .fileName(name).fileSize(6L).build();
        storedFiles.add(f);
        return f;
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private static MockMultipartFile docx(String name) {
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DOCX);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private void verifyNothingSaved() {
        verify(submissions, never()).save(any());
        verify(files, never()).insertFile(any(), any(), anyString(), anyLong(), any());
    }

    // ----- UC-64 Nộp bài lần đầu -----

    @Test
    void learnerSubmitsContentAndSeveralFilesAtOnce() {
        Assignment a = stored(NOW.plusDays(1));

        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "  Bài làm  ",
                List.of(pdf("C:\\bai lam/bai-1.PDF"), docx("bai 2.docx")));

        assertThat(r.content()).isEqualTo("Bài làm");
        assertThat(r.onTime()).isTrue();
        assertThat(r.files()).extracting(f -> f.fileName()).containsExactly("bai-1.pdf", "bai 2.docx");
        assertThat(r.files()).extracting(f -> f.fileType()).containsExactly("PDF", "DOCX");
        verify(files, times(2)).insertFile(any(), eq(r.submissionId()), anyString(), eq(6L), any());
    }

    @Test
    void submitNeedsContentOrFile() {
        Assignment a = stored(NOW.plusDays(1));
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "   ", List.of()),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), null, null),
                HttpStatus.BAD_REQUEST);
        verifyNothingSaved();
    }

    @Test
    void submitRejectsMoreThanTenFilesOrInvalidFile() {
        Assignment a = stored(NOW.plusDays(1));
        List<MultipartFile> eleven = new ArrayList<>(Collections.nCopies(6, pdf("a.pdf")));
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), null, eleven),
                HttpStatus.BAD_REQUEST);
        MockMultipartFile fake = new MockMultipartFile("file", "x.docx", "application/octet-stream", PDF);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), null,
                List.of(pdf("a.pdf"), fake)), HttpStatus.BAD_REQUEST);
        verifyNothingSaved();
    }

    @Test
    void secondSubmitIsRejectedLearnerMustEditInstead() {
        Assignment a = stored(NOW.plusDays(1));
        previous(a, 1, NOW.minusHours(2), false);
        assertThatThrownBy(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "x", null))
                .isInstanceOf(ClassroomException.class).hasMessageContaining("chỉnh sửa bài đã nộp");
        verifyNothingSaved();
    }

    @Test
    void lateSubmissionAllowedOnceWhenNothingSubmitted() {
        Assignment a = stored(NOW.minusDays(1));
        SubmissionResponse r = service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "Nộp muộn", null);
        assertThat(r.onTime()).isFalse();
    }

    @Test
    void submitAfterGradedBlockedAndReadOnlyBlocked() {
        Assignment a = stored(null);
        previous(a, 1, NOW.minusDays(2), true);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.CONFLICT);

        Assignment b = stored(NOW.plusDays(1));
        when(classrooms.isReadOnly(space)).thenReturn(true);
        assertStatus(() -> service.submit(learnerActor, space.getSpaceId(), b.getAssignmentId(), "x", null), HttpStatus.CONFLICT);
        verifyNothingSaved();
    }

    @Test
    void tutorCannotSubmit() {
        Assignment a = stored(NOW.plusDays(1));
        assertStatus(() -> service.submit(tutorActor, space.getSpaceId(), a.getAssignmentId(), "x", null), HttpStatus.FORBIDDEN);
    }

    // ----- UC-96 Sửa bài đã nộp -----

    @Test
    void learnerEditsSubmissionReplacingFiles() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(5), false);
        SubmissionFile old = storedFile(s, "cu.pdf");
        SubmissionFile keep = storedFile(s, "giu-lai.pdf");

        SubmissionResponse r = service.update(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                "Bản sửa", List.of(docx("moi.docx")), List.of(old.getFileId()));

        assertThat(r.content()).isEqualTo("Bản sửa");
        assertThat(r.files()).extracting(f -> f.fileName()).containsExactlyInAnyOrder("giu-lai.pdf", "moi.docx");
        assertThat(s.getSubmittedAt()).isEqualTo(NOW);
        assertThat(storedFiles).contains(keep).doesNotContain(old);
    }

    @Test
    void editBlockedAfterDeadlineOrGraded() {
        Assignment late = stored(NOW.minusDays(1));
        AssignmentSubmission s1 = previous(late, 1, NOW.minusDays(2), false);
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), late.getAssignmentId(), s1.getSubmissionId(),
                "x", null, null), HttpStatus.CONFLICT);

        history.clear();
        Assignment graded = stored(null);
        AssignmentSubmission s2 = previous(graded, 1, NOW.minusDays(2), true);
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), graded.getAssignmentId(), s2.getSubmissionId(),
                "x", null, null), HttpStatus.CONFLICT);
        verify(submissions, never()).save(any());
    }

    @Test
    void editCannotLeaveSubmissionEmpty() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(5), false);
        SubmissionFile only = storedFile(s, "bai.pdf");
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                "  ", null, List.of(only.getFileId())), HttpStatus.BAD_REQUEST);
        assertThat(storedFiles).contains(only);
    }

    @Test
    void editRejectsUnknownFileAndTooManyFiles() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(5), false);
        for (int i = 0; i < 4; i++) storedFile(s, "f" + i + ".pdf");
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                "x", null, List.of(UUID.randomUUID())), HttpStatus.NOT_FOUND);
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                "x", List.of(pdf("a.pdf"), pdf("b.pdf")), null), HttpStatus.BAD_REQUEST);
        verify(files, never()).insertFile(any(), any(), anyString(), anyLong(), any());
    }

    @Test
    void onlyCurrentSubmissionCanBeEdited() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission old = previous(a, 1, NOW.minusDays(2), false);
        previous(a, 2, NOW.minusDays(1), false);
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), a.getAssignmentId(), old.getSubmissionId(),
                "x", null, null), HttpStatus.FORBIDDEN);
    }

    // ----- UC-67 Chấm điểm -----

    @Test
    void gradeCurrentSubmissionAfterDeadline() {
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
    void gradeBeforeDeadlineBlockedAndOnlyOnce() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.CONFLICT);

        history.clear();
        Assignment b = stored(NOW.minusDays(1));
        AssignmentSubmission graded = previous(b, 1, NOW.minusDays(2), true);
        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), b.getAssignmentId(), graded.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.CONFLICT);
        verify(submissions, never()).save(any());
    }

    @Test
    void gradeOlderRowBlocked() {
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
        assertStatus(() -> service.grade(learnerActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                new GradeRequest(BigDecimal.TEN, null)), HttpStatus.FORBIDDEN);
    }

    // ----- Tệp bài làm -----

    @Test
    void readsOneFileOfSubmission() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        SubmissionFile f = storedFile(s, "bai 2.docx");
        when(files.findByFileIdAndSubmissionId(f.getFileId(), s.getSubmissionId())).thenReturn(Optional.of(f));
        when(files.findFileData(f.getFileId())).thenReturn(DOCX);

        SubmissionFileContent c = service.getFile(tutorActor, space.getSpaceId(), a.getAssignmentId(),
                s.getSubmissionId(), f.getFileId());

        assertThat(c.fileName()).isEqualTo("bai 2.docx");
        assertThat(c.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(c.data()).isEqualTo(DOCX);
    }

    @Test
    void unknownFileIsNotFound() {
        Assignment a = stored(NOW.plusDays(1));
        AssignmentSubmission s = previous(a, 1, NOW.minusHours(1), false);
        when(files.findByFileIdAndSubmissionId(any(), any())).thenReturn(Optional.empty());
        assertStatus(() -> service.getFile(tutorActor, space.getSpaceId(), a.getAssignmentId(), s.getSubmissionId(),
                UUID.randomUUID()), HttpStatus.NOT_FOUND);
        verify(files, never()).findFileData(any());
    }
}
