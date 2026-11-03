package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.LearningMaterialResponse;
import com.kizuna.features.classroom.dto.MaterialFileLinkResponse;
import com.kizuna.features.classroom.entity.LearningMaterial;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.MaterialStorageService;
import com.kizuna.features.classroom.service.impl.LearningMaterialServiceImpl;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private MaterialStorageService storage;
    private LearningMaterialServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private CurrentActor tutorActor;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        materials = mock(LearningMaterialRepository.class);
        storage = mock(MaterialStorageService.class);
        service = new LearningMaterialServiceImpl(classrooms, materials, storage, new MaterialFileValidator());

        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        space = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        tutorActor = asTutor(tutor);

        when(storage.isConfigured()).thenReturn(true);
        when(storage.signedUrlTtlSeconds()).thenReturn(300);
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
                .description("Mô tả cũ").fileUrl("spaces/" + space.getSpaceId() + "/old.pdf").fileType("PDF")
                .uploadedBy(tutor.getUser()).build();
        when(materials.findByMaterialIdAndSpace_SpaceId(m.getMaterialId(), space.getSpaceId())).thenReturn(Optional.of(m));
        return m;
    }

    // ----- UC-65 Upload -----

    @Test
    void uploadStoresFileThenSavesRecord() {
        LearningMaterialResponse r = service.upload(tutorActor, space.getSpaceId(), " Từ vựng bài 1 ", " Ôn tập ", pdf());

        ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(path.capture(), eq(PDF), eq("application/pdf"));
        assertThat(path.getValue()).startsWith("spaces/" + space.getSpaceId() + "/").endsWith(".pdf")
                .doesNotContain("bai-1");
        verify(materials).save(argThat(m -> m.getFileUrl().equals(path.getValue()) && m.getFileType().equals("PDF")
                && m.getSpace() == space && m.getUploadedBy() == tutor.getUser()));
        assertThat(r.title()).isEqualTo("Từ vựng bài 1");
        assertThat(r.description()).isEqualTo("Ôn tập");
        assertThat(r.fileType()).isEqualTo("PDF");
    }

    @Test
    void uploadBlankDescriptionStoredAsNull() {
        LearningMaterialResponse r = service.upload(tutorActor, space.getSpaceId(), "Bài 1", "   ", pdf());
        assertThat(r.description()).isNull();
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
        verifyNoUploadOrSave();
    }

    @Test
    void uploadBlockedWhenClassroomNotManageable() {
        CurrentActor learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(learnerActor, space.getSpaceId()))
                .thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.upload(learnerActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.FORBIDDEN);

        Tutor expired = tutor(TutorMembershipStatus.EXPIRED);
        CurrentActor expiredActor = asTutor(expired);
        when(classrooms.loadManageableSpace(expiredActor, space.getSpaceId())).thenThrow(ClassroomException.conflict("x"));
        assertStatus(() -> service.upload(expiredActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.CONFLICT);
        verifyNoUploadOrSave();
    }

    @Test
    void uploadFailsCleanlyWhenStorageNotConfigured() {
        when(storage.isConfigured()).thenReturn(false);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoUploadOrSave();
    }

    @Test
    void uploadDeletesStoredFileWhenSaveFails() {
        when(materials.save(any())).thenThrow(new RuntimeException("db down"));
        assertThatThrownBy(() -> service.upload(tutorActor, space.getSpaceId(), "Bài", null, pdf()))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<String> uploaded = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(uploaded.capture(), any(), any());
        verify(storage).delete(uploaded.getValue());
    }

    @Test
    void uploadNotSavedWhenStorageFails() {
        doThrow(ClassroomException.storageUnavailable()).when(storage).upload(any(), any(), any());
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), "Bài", null, pdf()), HttpStatus.SERVICE_UNAVAILABLE);
        verify(materials, never()).save(any());
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
    }

    @Test
    void listDeniedForOutsiders() {
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.list(outsider, space.getSpaceId()), HttpStatus.FORBIDDEN);
        verify(materials, never()).findBySpace_SpaceIdOrderByCreatedAtDesc(any());
    }

    @Test
    void fileLinkIsSignedForParticipant() {
        LearningMaterial m = existing();
        when(storage.createSignedUrl(m.getFileUrl())).thenReturn("https://signed");

        MaterialFileLinkResponse link = service.getFileLink(asLearner(learner), space.getSpaceId(), m.getMaterialId());

        assertThat(link.url()).isEqualTo("https://signed");
        assertThat(link.expiresInSeconds()).isEqualTo(300);
    }

    @Test
    void fileLinkDeniedForOutsiderAndForMaterialOfOtherClassroom() {
        CurrentActor outsider = asLearner(learner());
        when(classrooms.loadAccessibleSpace(outsider, space.getSpaceId())).thenThrow(ClassroomException.forbidden("x"));
        assertStatus(() -> service.getFileLink(outsider, space.getSpaceId(), UUID.randomUUID()), HttpStatus.FORBIDDEN);

        when(materials.findByMaterialIdAndSpace_SpaceId(any(), eq(space.getSpaceId()))).thenReturn(Optional.empty());
        assertStatus(() -> service.getFileLink(tutorActor, space.getSpaceId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
        verify(storage, never()).createSignedUrl(any());
    }

    // ----- UC-94 Update -----

    @Test
    void updateMetadataOnlyKeepsFile() {
        LearningMaterial m = existing();
        String oldPath = m.getFileUrl();

        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", "", null);

        assertThat(r.title()).isEqualTo("Mới");
        assertThat(r.description()).isNull();
        assertThat(m.getFileUrl()).isEqualTo(oldPath);
        verify(storage, never()).upload(any(), any(), any());
        verify(storage, never()).delete(any());
    }

    @Test
    void updateReplacesFileAndDeletesOldAfterSave() {
        LearningMaterial m = existing();
        String oldPath = m.getFileUrl();

        LearningMaterialResponse r = service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", null,
                new MockMultipartFile("file", "hinh.png", "image/png", PNG));

        assertThat(r.fileType()).isEqualTo("PNG");
        assertThat(m.getFileUrl()).isNotEqualTo(oldPath).endsWith(".png");
        var order = inOrder(storage, materials);
        order.verify(storage).upload(eq(m.getFileUrl()), eq(PNG), eq("image/png"));
        order.verify(materials).save(m);
        order.verify(storage).delete(oldPath);
    }

    @Test
    void updateWithInvalidReplacementKeepsOriginal() {
        LearningMaterial m = existing();
        assertStatus(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", null,
                new MockMultipartFile("file", "video.mp4", "video/mp4", PDF)), HttpStatus.BAD_REQUEST);
        assertThat(m.getTitle()).isEqualTo("Cũ");
        verifyNoUploadOrSave();
    }

    @Test
    void updateSaveFailureRemovesNewFileAndKeepsOld() {
        LearningMaterial m = existing();
        String oldPath = m.getFileUrl();
        when(materials.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.update(tutorActor, space.getSpaceId(), m.getMaterialId(), "Mới", null, pdf()))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<String> uploaded = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(uploaded.capture(), any(), any());
        verify(storage).delete(uploaded.getValue());
        verify(storage, never()).delete(oldPath);
        assertThat(m.getFileUrl()).isEqualTo(oldPath);
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
        verifyNoUploadOrSave();
    }

    private void verifyNoUploadOrSave() {
        verify(storage, never()).upload(any(), any(), any());
        verify(materials, never()).save(any());
    }
}
