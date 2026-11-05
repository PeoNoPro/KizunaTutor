package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.progress.dto.AssignmentAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.impl.AssignmentAttachmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssignmentAttachmentServiceImplTest {

    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};

    private ClassroomService classrooms;
    private AssignmentRepository assignments;
    private AssignmentSubmissionRepository submissions;
    private AssignmentAttachmentServiceImpl service;

    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        assignments = mock(AssignmentRepository.class);
        submissions = mock(AssignmentSubmissionRepository.class);
        service = new AssignmentAttachmentServiceImpl(classrooms, assignments, submissions, new MaterialFileValidator());

        Tutor tutor = tutor();
        Learner learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        assignment = assignment(space, NOW.plusDays(2));

        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(assignments.findByAssignmentIdAndSpace_SpaceId(assignment.getAssignmentId(), space.getSpaceId()))
                .thenReturn(Optional.of(assignment));
        when(assignments.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "C:\\de bai/Bai-1.PDF", "application/pdf", PDF);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void tutorUploadsAttachmentWithCleanName() {
        AssignmentAttachmentResponse r = service.upload(tutorActor, space.getSpaceId(), assignment.getAssignmentId(), pdf());

        assertThat(r.attachmentName()).isEqualTo("Bai-1.pdf");
        assertThat(r.attachmentType()).isEqualTo("PDF");
        assertThat(assignment.getAttachmentUrl()).isEqualTo("Bai-1.pdf");
        verify(assignments).updateFileData(assignment.getAssignmentId(), PDF);
    }

    @Test
    void uploadRejectsLearnerInvalidFileAndGradedAssignment() {
        UUID s = space.getSpaceId();
        UUID a = assignment.getAssignmentId();
        assertStatus(() -> service.upload(learnerActor, s, a, pdf()), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.upload(tutorActor, s, a, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, s, a,
                new MockMultipartFile("file", "virus.pdf", "application/pdf", new byte[]{0x4D, 0x5A})), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, s, UUID.randomUUID(), pdf()), HttpStatus.NOT_FOUND);

        when(submissions.existsByAssignment_AssignmentIdAndFinalizedTrue(a)).thenReturn(true);
        assertStatus(() -> service.upload(tutorActor, s, a, pdf()), HttpStatus.CONFLICT);
        verify(assignments, never()).updateFileData(any(), any());
    }

    @Test
    void uploadBlockedWhenClassroomReadOnly() {
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId()))
                .thenThrow(ClassroomException.conflict("Lớp học đang ở chế độ chỉ xem."));
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), assignment.getAssignmentId(), pdf()),
                HttpStatus.CONFLICT);
    }

    @Test
    void tutorRemovesAttachment() {
        assignment.setAttachmentUrl("Bai-1.pdf");
        AssignmentAttachmentResponse r = service.remove(tutorActor, space.getSpaceId(), assignment.getAssignmentId());

        assertThat(r.attachmentName()).isNull();
        assertThat(assignment.getAttachmentUrl()).isNull();
        verify(assignments).updateFileData(assignment.getAssignmentId(), null);
    }

    @Test
    void removeWithoutAttachmentIsNotFound() {
        assertStatus(() -> service.remove(tutorActor, space.getSpaceId(), assignment.getAssignmentId()),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void learnerOfClassroomReadsAttachment() {
        assignment.setAttachmentUrl("Bai-1.pdf");
        when(assignments.findFileData(assignment.getAssignmentId())).thenReturn(PDF);

        SubmissionFileContent file = service.getFile(learnerActor, space.getSpaceId(), assignment.getAssignmentId());

        assertThat(file.data()).isEqualTo(PDF);
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.fileName()).isEqualTo("Bai-1.pdf");
    }

    @Test
    void readingMissingAttachmentIsNotFound() {
        assertStatus(() -> service.getFile(learnerActor, space.getSpaceId(), assignment.getAssignmentId()),
                HttpStatus.NOT_FOUND);
    }
}
