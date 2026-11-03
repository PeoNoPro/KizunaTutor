package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileContent;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
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
    private static final byte[] PNG = MaterialFileValidatorTest.PNG;

    private ClassroomService classrooms;
    private LearningMaterialRepository materials;
    private LearningMaterialServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private CurrentActor tutorActor;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        materials = mock(LearningMaterialRepository.class);
        service = new LearningMaterialServiceImpl(classrooms, materials, new MaterialFileValidator());

        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        space = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        tutorActor = asTutor(tutor);

        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(materials.save(any(LearningMaterial.class))).thenAnswer(inv -> {
            LearningMaterial m = inv.getArgument(0);
            if (m.getMaterialId() == null) m.setMaterialId(UUID.randomUUID());
            return m;
        });
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "bai-1.pdf", "application/pdf", PDF);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private LearningMaterial existing() {
        LearningMaterial m = LearningMaterial.builder().materialId(UUID.randomUUID()).space(space).title("Cũ")
                .description("Mô tả cũ").fileUrl("cu.pdf").fileType("PDF").uploadedBy(tutor.getUser()).build();
        when(materials.findByMaterialIdAndSpace_SpaceId(m.getMaterialId(), space.getSpaceId())).thenReturn(Optional.of(m));
        return m;
    }

    private void verifyNothingWritten() {
        verify(materials, never()).save(any());
        verify(materials, never()).updateFileData(any(), any());
    }

    // ----- UC-65 Upload -----

    @Test
    void uploadSavesRecordThenFileBytes() {
        LearningMaterialResponse r = service.upload(tutorActor, space.getSpaceId(), " Từ vựng bài 1 ", " Ôn tập ", pdf());

        var order = inOrder(materials);
        order.verify(materials).save(argThat(m -> m.getFileUrl().equals("bai-1.pdf") && m.getFileType().equals("PDF")
                && m.getSpace() == space && m.getUploadedBy() == tutor.getUser()));
        order.verify(materials).updateFileData(r.materialId(), PDF);
        assertThat(r.title()).isEqualTo("Từ vựng bài 1");
        assertThat(r.description()).isEqualTo("Ôn tập");
        assertThat(r.fileType()).isEqualTo("PDF");
    }

    @Test
    void uploadBlankDescriptionStoredAsNull() {
        assertThat(service.upload(tutorActor, space.getSpaceId(), "Bài 1", "   ", pdf()).description()).isNull();
    }

    @Test
    void uploadRejectsMissingTitleOrFileOrLongDescription() {
        UUID id = space.getSpaceId();
        assertStatus(() -> service.upload(tutorActor, id, "  ", null, pdf()), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, id, "a".repeat(151), null, pdf()), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, id, "Bài 1", "a".repeat(1001), pdf()), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, id, "Bài 1", null, null), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(tutorActor, id, "Bài 1", null,
                new MockMultipartFile("file", "virus.pdf", "application/pdf", new byte[]{0x4D, 0x5A, 0, 0})), HttpStatus.BAD_REQUEST);
        verifyNothingWritten();
    }

    @Test
    void uploadBlockedWhenClassroomNotManageable() {
        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.upload(learnerActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.FORBIDDEN);

        CurrentActor expiredActor = asTutor(tutor(TutorMembershipStatus.EXPIRED));
        when(classrooms.loadManageableSpace(expiredActor, space.getSpaceId())).thenThrow(ClassroomException.conflict("x"));
        assertStatus(() -> service.upload(expiredActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.CONFLICT);
        verifyNothingWritten();
    }

    @Test
    void cleanFileNameStripsPathAndUnsafeCharacters() {
        var pdf = MaterialFileValidator.MaterialFileType.PDF;
        var jpg = MaterialFileValidator.MaterialFileType.JPG;
        assertThat(LearningMaterialServiceImplTest.clean("C:\\Users\\a\\Bài 1.PDF", pdf)).isEqualTo("Bài 1.pdf");
        assertThat(clean("../../etc/pa<ss>wd.pdf", pdf)).isEqualTo("passwd.pdf");
        assertThat(clean("anh.jpeg", jpg)).isEqualTo("anh.jpg");
        assertThat(clean(null, pdf)).isEqualTo("tai-lieu.pdf");
        assertThat(clean(".pdf", pdf)).isEqualTo("tai-lieu.pdf");
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
    void listReturnsMaterialsForParticipants() {
        LearningMaterial m = existing();
        when(materials.findBySpace_SpaceIdOrderByCreatedAtDesc(space.getSpaceId())).thenReturn(List.of(m));
        CurrentActor learnerActor = asLearner(learner);

        List<LearningMaterialResponse> result = service.list(learnerActor, space.getSpaceId());

        verify(classrooms).loadAccessibleSpace(learnerActor, space.getSpaceId());
        assertThat(result).extracting(LearningMaterialResponse::title).containsExactly("Cũ");
        assertThat(result.get(0).uploadedByName()).isEqualTo("Gia sư A");
        verify(materials, never()).findFileData(any());
    }

    @Test
    void listDeniedForOutsiders() {
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.list(outsider, space.getSpaceId()), HttpStatus.FORBIDDEN);
        verify(materials, never()).findBySpace_SpaceIdOrderByCreatedAtDesc(any());
    }

    @Test
    void fileReturnedForParticipantWithContentType() {
        LearningMaterial m = existing();
        when(materials.findFileData(m.getMaterialId())).thenReturn(PDF);

        MaterialFileContent file = service.getFile(asLearner(learner), space.getSpaceId(), m.getMaterialId());

        assertThat(file.data()).isEqualTo(PDF);
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.fileName()).isEqualTo("cu.pdf");
    }

    @Test
    void fileDeniedForOutsiderMissingForOtherClassroomOrEmpty() {
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.getFile(outsider, space.getSpaceId(), UUID.randomUUID()), HttpStatus.FORBIDDEN);

        when(materials.findByMaterialIdAndSpace_SpaceId(any(), eq(space.getSpaceId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.getFile(tutorActor, space.getSpaceId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
        verify(materials, never()).findFileData(any());

        LearningMaterial m = existing();
        when(materials.findFileData(m.getMaterialId())).thenReturn(null);
        assertStatus(() -> service.getFile(tutorActor, space.getSpaceId(), m.getMaterialId()), HttpStatus.NOT_FOUND);
    }

    // ----- UC-94 Update -----

    @Test
    void updateMetadataOnlyKeepsFile() {
        LearningMaterial m = existing();

        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", "", null);

        assertThat(r.title()).isEqualTo("Mới");
        assertThat(r.description()).isNull();
        assertThat(m.getFileUrl()).isEqualTo("cu.pdf");
        verify(materials).save(m);
        verify(materials, never()).updateFileData(any(), any());
    }

    @Test
    void updateReplacesFileContentAndName() {
        LearningMaterial m = existing();

        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", null,
                new MockMultipartFile("file", "hinh.png", "image/png", PNG));

        assertThat(r.fileType()).isEqualTo("PNG");
        assertThat(m.getFileUrl()).isEqualTo("hinh.png");
        var order = inOrder(materials);
        order.verify(materials).save(m);
        order.verify(materials).updateFileData(m.getMaterialId(), PNG);
    }

    @Test
    void updateWithInvalidReplacementKeepsOriginal() {
        LearningMaterial m = existing();
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", null,
                new MockMultipartFile("file", "video.mp4", "video/mp4", PDF)), HttpStatus.BAD_REQUEST);
        assertThat(m.getTitle()).isEqualTo("Cũ");
        assertThat(m.getFileUrl()).isEqualTo("cu.pdf");
        verifyNothingWritten();
    }

    @Test
    void updateRejectsMaterialOfOtherClassroomAndNonManagers() {
        when(materials.findByMaterialIdAndSpace_SpaceId(any(), eq(space.getSpaceId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), UUID.randomUUID(), "Mới", null, null),
                HttpStatus.NOT_FOUND);

        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.update(learnerActor, space.getSpaceId(), UUID.randomUUID(), "Mới", null, null),
                HttpStatus.FORBIDDEN);
        verifyNothingWritten();
    }
}
