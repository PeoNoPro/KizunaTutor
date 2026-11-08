package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.progress.dto.FileAttachmentResponse;
import com.kizuna.features.progress.dto.SubmissionFileContent;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentFile;
import com.kizuna.features.progress.repository.AssignmentFileRepository;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.service.impl.AssignmentAttachmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssignmentAttachmentServiceImplTest {

    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31};
    private static final byte[] DOC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    private AssignmentFileRepository fileRepo;
    private AssignmentAttachmentServiceImpl service;

    private LearningSpace space;
    private CurrentActor learnerActor;
    private Assignment assignment;
    private List<AssignmentFile> stored;

    @BeforeEach
    void setUp() {
        ClassroomService classrooms = mock(ClassroomService.class);
        AssignmentRepository assignments = mock(AssignmentRepository.class);
        fileRepo = mock(AssignmentFileRepository.class);
        service = new AssignmentAttachmentServiceImpl(classrooms, assignments, fileRepo, new MaterialFileValidator());

        Tutor tutor = tutor();
        Learner learner = learner();
        space = space(tutor, learner);
        learnerActor = asLearner(learner);
        assignment = assignment(space, NOW.plusDays(2));
        stored = new ArrayList<>();

        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(assignments.findByAssignmentIdAndSpace_SpaceId(assignment.getAssignmentId(), space.getSpaceId()))
                .thenReturn(Optional.of(assignment));
        when(fileRepo.insertFile(any(), any(), anyString(), anyLong(), any())).thenAnswer(inv -> {
            stored.add(AssignmentFile.builder().fileId(inv.getArgument(0)).assignmentId(inv.getArgument(1))
                    .fileName(inv.getArgument(2)).fileSize(inv.getArgument(3)).build());
            return 1;
        });
        when(fileRepo.findByAssignmentIdOrderByCreatedAtAscFileNameAsc(assignment.getAssignmentId()))
                .thenAnswer(inv -> List.copyOf(stored));
        doAnswer(inv -> {
            Iterable<AssignmentFile> gone = inv.getArgument(0);
            gone.forEach(stored::remove);
            return null;
        }).when(fileRepo).deleteAll(anyIterable());
    }

    private AssignmentFile existing(String name) {
        AssignmentFile f = AssignmentFile.builder().fileId(UUID.randomUUID()).assignmentId(assignment.getAssignmentId())
                .fileName(name).fileSize(6L).build();
        stored.add(f);
        when(fileRepo.findByFileIdAndAssignmentId(f.getFileId(), assignment.getAssignmentId())).thenReturn(Optional.of(f));
        return f;
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private void verifyNothingWritten() {
        verify(fileRepo, never()).insertFile(any(), any(), anyString(), anyLong(), any());
        verify(fileRepo, never()).deleteAll(anyIterable());
    }

    @Test
    void addsSeveralFilesWithCleanNames() {
        List<FileAttachmentResponse> r = service.applyChanges(assignment,
                List.of(pdf("C:\\de bai/Bai-1.PDF"), new MockMultipartFile("file", "Tu vung.doc", "application/msword", DOC)), null);

        assertThat(r).extracting(FileAttachmentResponse::fileName).containsExactly("Bai-1.pdf", "Tu vung.doc");
        assertThat(r).extracting(FileAttachmentResponse::fileType).containsExactly("PDF", "DOC");
    }

    @Test
    void oneBadFileRejectsAllAndNamesTheFile() {
        MockMultipartFile fake = new MockMultipartFile("file", "gia mao.doc", "application/msword", PDF);
        assertThatThrownBy(() -> service.applyChanges(assignment, List.of(pdf("ok-1.pdf"), fake, pdf("ok-2.pdf")), null))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).startsWith("Tệp \"gia mao.doc\": ");
                    assertThat(e.getFieldErrors()).containsKey("file");
                });
        verifyNothingWritten();
    }

    @Test
    void replaceAllFiveWithoutContentIsValidBecauseOnlyFinalResultCounts() {
        assignment.setDescription(null);
        List<UUID> all = new ArrayList<>();
        for (int i = 0; i < 5; i++) all.add(existing("cu" + i + ".pdf").getFileId());

        List<FileAttachmentResponse> r = service.applyChanges(assignment,
                List.of(pdf("m0.pdf"), pdf("m1.pdf"), pdf("m2.pdf"), pdf("m3.pdf"), pdf("m4.pdf")), all);

        assertThat(r).extracting(FileAttachmentResponse::fileName).containsExactly("m0.pdf", "m1.pdf", "m2.pdf", "m3.pdf", "m4.pdf");
    }

    @Test
    void rejectsEmptyResultWithoutContentMoreThanFiveAndUnknownFile() {
        assignment.setDescription("  ");
        assertStatus(() -> service.applyChanges(assignment, List.of(), null), HttpStatus.BAD_REQUEST);
        UUID only = existing("de.pdf").getFileId();
        assertStatus(() -> service.applyChanges(assignment, null, List.of(only)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.applyChanges(assignment,
                List.of(pdf("a.pdf"), pdf("b.pdf"), pdf("c.pdf"), pdf("d.pdf"), pdf("e.pdf")), null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.applyChanges(assignment, null, List.of(UUID.randomUUID())), HttpStatus.NOT_FOUND);
        assertThat(stored).hasSize(1);
        verifyNothingWritten();
    }

    @Test
    void contentOnlyWithoutFilesIsValid() {
        assignment.setDescription("Viết 10 câu.");
        assertThat(service.applyChanges(assignment, null, null)).isEmpty();
    }

    @Test
    void learnerReadsAttachment() {
        AssignmentFile f = existing("Tu vung.doc");
        when(fileRepo.findFileData(f.getFileId())).thenReturn(DOC);
        SubmissionFileContent c = service.getFile(learnerActor, space.getSpaceId(), assignment.getAssignmentId(), f.getFileId());
        assertThat(c.fileName()).isEqualTo("Tu vung.doc");
        assertThat(c.contentType()).isEqualTo("application/msword");
        assertThat(c.data()).isEqualTo(DOC);
        assertStatus(() -> service.getFile(learnerActor, space.getSpaceId(), assignment.getAssignmentId(), UUID.randomUUID()),
                HttpStatus.NOT_FOUND);
    }
}
