package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.dto.MaterialFileResponse;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningMaterialFile;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningMaterialFileRepository;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.impl.LearningMaterialServiceImpl;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LearningMaterialServiceImplTest {

    private static final byte[] PDF = MaterialFileValidatorTest.PDF;
    private static final byte[] DOCX = MaterialFileValidatorTest.DOCX;

    private ClassroomService classrooms;
    private LearningMaterialRepository materials;
    private LearningMaterialFileRepository files;
    private LearningMaterialServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private CurrentActor tutorActor;
    /** Giả lập bảng learning_material_files. */
    private List<LearningMaterialFile> stored;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        materials = mock(LearningMaterialRepository.class);
        files = mock(LearningMaterialFileRepository.class);
        service = new LearningMaterialServiceImpl(classrooms, materials, files, new MaterialFileValidator());

        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        space = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        tutorActor = asTutor(tutor);
        stored = new ArrayList<>();

        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(materials.save(any(LearningMaterial.class))).thenAnswer(inv -> {
            LearningMaterial m = inv.getArgument(0);
            if (m.getMaterialId() == null) m.setMaterialId(UUID.randomUUID());
            return m;
        });
        when(files.insertFile(any(), any(), anyString(), anyLong(), any())).thenAnswer(inv -> {
            stored.add(LearningMaterialFile.builder().fileId(inv.getArgument(0)).materialId(inv.getArgument(1))
                    .fileName(inv.getArgument(2)).fileSize(inv.getArgument(3)).build());
            return 1;
        });
        when(files.findByMaterialIdOrderByCreatedAtAscFileNameAsc(any())).thenAnswer(inv ->
                stored.stream().filter(f -> f.getMaterialId().equals(inv.getArgument(0))).toList());
        doAnswer(inv -> {
            Iterable<LearningMaterialFile> gone = inv.getArgument(0);
            gone.forEach(stored::remove);
            return null;
        }).when(files).deleteAll(anyIterable());
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }

    private static MockMultipartFile docx(String name) {
        return new MockMultipartFile("file", name, "application/octet-stream", DOCX);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private LearningMaterial existing(String... fileNames) {
        LearningMaterial m = LearningMaterial.builder().materialId(UUID.randomUUID()).space(space).title("Cũ")
                .description("Mô tả cũ").fileUrl(fileNames.length > 0 ? fileNames[0] : "cu.pdf").fileType("PDF")
                .uploadedBy(tutor.getUser()).build();
        when(materials.findByMaterialIdAndSpace_SpaceId(m.getMaterialId(), space.getSpaceId())).thenReturn(Optional.of(m));
        for (String name : fileNames) {
            LearningMaterialFile f = LearningMaterialFile.builder().fileId(UUID.randomUUID())
                    .materialId(m.getMaterialId()).fileName(name).fileSize(6L).build();
            stored.add(f);
            when(files.findByFileIdAndMaterialId(f.getFileId(), m.getMaterialId())).thenReturn(Optional.of(f));
        }
        return m;
    }

    private void verifyNothingWritten() {
        verify(materials, never()).save(any());
        verify(files, never()).insertFile(any(), any(), anyString(), anyLong(), any());
    }

    // ----- UC-65 Upload: một tài liệu nhiều tệp -----

    @Test
    void uploadCreatesOneMaterialWithAllFiles() {
        LearningMaterialResponse r = service.upload(tutorActor, space.getSpaceId(), " Ngữ pháp ", " Ôn tập ",
                List.of(pdf("C:\\Users\\a\\Bai 1.PDF"), docx("Bai 2.docx")));

        assertThat(r.title()).isEqualTo("Ngữ pháp");
        assertThat(r.description()).isEqualTo("Ôn tập");
        assertThat(r.files()).extracting(MaterialFileResponse::fileName).containsExactly("Bai 1.pdf", "Bai 2.docx");
        assertThat(r.files()).extracting(MaterialFileResponse::fileType).containsExactly("PDF", "DOCX");
        verify(materials, times(1)).save(argThat(m -> m.getFileUrl().equals("Bai 1.pdf") && m.getFileType().equals("PDF")
                && m.getSpace() == space && m.getUploadedBy() == tutor.getUser()));
        verify(files, times(2)).insertFile(any(), eq(r.materialId()), anyString(), anyLong(), any());
    }

    @Test
    void uploadAlwaysRequiresTitleAndAtLeastOneFile() {
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "  ", null, List.of(pdf("a.pdf"), pdf("b.pdf"))),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Ngữ pháp", null, List.of()), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Ngữ pháp", "x".repeat(1001), List.of(pdf("a.pdf"))),
                HttpStatus.BAD_REQUEST);
        verifyNothingWritten();
    }

    @Test
    void uploadRejectsBadFileOrMoreThanTenWithoutSavingAnything() {
        MockMultipartFile fake = new MockMultipartFile("file", "gia.docx", "application/octet-stream", PDF);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Ngữ pháp", null, List.of(pdf("a.pdf"), fake)),
                HttpStatus.BAD_REQUEST);
        List<MultipartFile> eleven = new ArrayList<>(Collections.nCopies(6, pdf("a.pdf")));
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Ngữ pháp", null, eleven), HttpStatus.BAD_REQUEST);
        verifyNothingWritten();
    }

    @Test
    void uploadBlockedWhenClassroomNotManageable() {
        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.upload(learnerActor, space.getSpaceId(), "T", null, List.of(pdf("a.pdf"))), HttpStatus.FORBIDDEN);
        verifyNothingWritten();
    }

    @Test
    void cleanFileNameStripsPathAndUnsafeCharacters() {
        var pdf = MaterialFileValidator.MaterialFileType.PDF;
        var jpg = MaterialFileValidator.MaterialFileType.JPG;
        assertThat(clean("C:\\Users\\a\\Bài 1.PDF", pdf)).isEqualTo("Bài 1.pdf");
        assertThat(clean("../../etc/pa<ss>wd.pdf", pdf)).isEqualTo("passwd.pdf");
        assertThat(clean("anh.jpeg", jpg)).isEqualTo("anh.jpg");
        assertThat(clean(null, pdf)).isEqualTo("tai-lieu.pdf");
        assertThat(clean("a".repeat(400) + ".pdf", pdf)).hasSize(204);
    }

    private static String clean(String name, MaterialFileValidator.MaterialFileType type) {
        try {
            var m = LearningMaterialServiceImpl.class.getDeclaredMethod("cleanFileName", String.class,
                    MaterialFileValidator.MaterialFileType.class);
            m.setAccessible(true);
            return (String) m.invoke(null, name, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ----- UC-61 View -----

    @Test
    void listReturnsMaterialsWithTheirFiles() {
        LearningMaterial m = existing("a.pdf", "b.docx");
        when(materials.findBySpace_SpaceIdOrderByCreatedAtDesc(space.getSpaceId())).thenReturn(List.of(m));
        when(files.findByMaterialIdInOrderByCreatedAtAscFileNameAsc(List.of(m.getMaterialId()))).thenReturn(List.copyOf(stored));

        List<LearningMaterialResponse> r = service.list(asLearner(learner), space.getSpaceId());

        assertThat(r).singleElement().satisfies(x ->
                assertThat(x.files()).extracting(MaterialFileResponse::fileName).containsExactly("a.pdf", "b.docx"));
    }

    @Test
    void readsOneFileOfMaterialWithContentType() {
        LearningMaterial m = existing("a.pdf", "b.docx");
        LearningMaterialFile docxFile = stored.get(1);
        when(files.findFileData(docxFile.getFileId())).thenReturn(DOCX);

        MaterialFileContent c = service.getFile(asLearner(learner), space.getSpaceId(), m.getMaterialId(), docxFile.getFileId());

        assertThat(c.fileName()).isEqualTo("b.docx");
        assertThat(c.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertStatus(() -> service.getFile(asLearner(learner), space.getSpaceId(), m.getMaterialId(), UUID.randomUUID()),
                HttpStatus.NOT_FOUND);
    }

    // ----- UC-94 Update: thay / xóa / thêm tệp -----

    @Test
    void updateMetadataOnlyKeepsFiles() {
        LearningMaterial m = existing("a.pdf");
        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", "", null, null);
        assertThat(r.title()).isEqualTo("Mới");
        assertThat(r.description()).isNull();
        assertThat(r.files()).extracting(MaterialFileResponse::fileName).containsExactly("a.pdf");
        verify(files, never()).insertFile(any(), any(), anyString(), anyLong(), any());
    }

    @Test
    void updateReplacesOneFileAndAddsAnother() {
        LearningMaterial m = existing("a.pdf", "b.pdf");
        UUID replaceA = stored.get(0).getFileId();

        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Ngữ pháp", null,
                List.of(pdf("a-moi.pdf"), docx("c.docx")), List.of(replaceA));

        assertThat(r.files()).extracting(MaterialFileResponse::fileName).containsExactly("b.pdf", "a-moi.pdf", "c.docx");
        assertThat(m.getFileUrl()).isEqualTo("b.pdf");
    }

    @Test
    void updateCannotRemoveAllFilesOrExceedTenOrRemoveUnknownFile() {
        LearningMaterial m = existing("a.pdf");
        UUID only = stored.get(0).getFileId();
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "T", null, null, List.of(only)),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "T", null, null,
                List.of(UUID.randomUUID())), HttpStatus.NOT_FOUND);
        List<MultipartFile> ten = new ArrayList<>(Collections.nCopies(5, pdf("x.pdf")));
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "T", null, ten, null),
                HttpStatus.BAD_REQUEST);
        assertThat(stored).hasSize(1);
        verify(materials, never()).save(any());
    }

    @Test
    void updateRejectsMaterialOfOtherClassroomAndNonManagers() {
        when(materials.findByMaterialIdAndSpace_SpaceId(any(), eq(space.getSpaceId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), UUID.randomUUID(), "T", null, null, null),
                HttpStatus.NOT_FOUND);
        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), UUID.randomUUID(), "T", null, null, null),
                HttpStatus.FORBIDDEN);
    }

    // ----- Delete Learning Material -----

    @Test
    void deleteRemovesMaterialOfClassroom() {
        LearningMaterial m = existing("a.pdf");
        service.delete(tutorActor, space.getSpaceId(), m.getMaterialId());
        verify(materials).delete(m);
    }

    @Test
    void deleteBlockedForNonManagersAndReadOnlyClassroom() {
        LearningMaterial m = existing("a.pdf");
        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.delete(learnerActor, space.getSpaceId(), m.getMaterialId()), HttpStatus.FORBIDDEN);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenThrow(ClassroomException.conflict("x"));
        assertStatus(() -> service.delete(tutorActor, space.getSpaceId(), m.getMaterialId()), HttpStatus.CONFLICT);
        verify(materials, never()).delete(any());
    }
}
