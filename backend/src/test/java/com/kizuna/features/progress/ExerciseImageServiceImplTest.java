package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageContent;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageInfo;
import com.kizuna.features.progress.entity.ExerciseImage;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseImageRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.impl.ExerciseImageServiceImpl;
import com.kizuna.features.storage.R2FileStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ảnh minh họa của câu hỏi bài làm trực tiếp: lưu DB khi tắt R2, lên R2 khi bật; kiểm tra định dạng và quyền xem. */
class ExerciseImageServiceImplTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    private ClassroomService classrooms;
    private ExerciseImageRepository images;
    private OnlineExerciseRepository exercises;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        images = mock(ExerciseImageRepository.class);
        exercises = mock(OnlineExerciseRepository.class);
        Tutor tutor = tutor();
        Learner learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(eq(tutorActor), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
    }

    private ExerciseImageServiceImpl service(R2FileStore r2) {
        return new ExerciseImageServiceImpl(classrooms, images, exercises, r2, CLOCK);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void withoutR2TheImageIsStoredInTheDatabase() {
        ImageInfo info = service(R2FileStore.disabled())
                .upload(tutorActor, space.getSpaceId(), new MockMultipartFile("file", "../Hình 1.png", "image/png", PNG));

        assertThat(info.fileName()).isEqualTo("Hình 1.png");
        verify(images).insertImage(eq(info.imageId()), eq(space.getSpaceId()), eq("Hình 1.png"), eq((long) PNG.length),
                eq("image/png"), eq(PNG));
    }

    @Test
    void withR2TheImageGoesToR2AndTheDatabaseColumnStaysEmpty() {
        S3Client s3 = mock(S3Client.class);
        ImageInfo info = service(new R2FileStore(s3, "kizunatutor-storage"))
                .upload(tutorActor, space.getSpaceId(), new MockMultipartFile("file", "a.png", "image/png", PNG));

        verify(s3).putObject(argThat((PutObjectRequest r) -> ("exercise-images/" + info.imageId()).equals(r.key())
                && "image/png".equals(r.contentType())), any(RequestBody.class));
        verify(images).insertImage(eq(info.imageId()), any(), any(), anyLong(), eq("image/png"), isNull());
    }

    @Test
    void onlyRealImagesUpTo5MbFromTheClassTutorAreAccepted() {
        ExerciseImageServiceImpl service = service(R2FileStore.disabled());
        // Đuôi .png nhưng nội dung không phải ảnh
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(),
                new MockMultipartFile("file", "x.png", "image/png", "<script>alert(1)</script>".getBytes())), HttpStatus.BAD_REQUEST);
        byte[] big = new byte[(int) ExerciseImageServiceImpl.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(),
                new MockMultipartFile("file", "big.png", "image/png", big)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.upload(learnerActor, space.getSpaceId(),
                new MockMultipartFile("file", "a.png", "image/png", PNG)), HttpStatus.FORBIDDEN);
        verify(images, never()).insertImage(any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void learnerSeesTheImageOnlyWhenItsExerciseIsPublishedAndOpen() {
        UUID exId = UUID.randomUUID();
        ExerciseImage img = ExerciseImage.builder().imageId(UUID.randomUUID()).spaceId(space.getSpaceId())
                .exerciseId(exId).fileName("a.png").fileSize((long) PNG.length).contentType("image/png").build();
        when(images.findByImageIdAndSpaceId(img.getImageId(), space.getSpaceId())).thenReturn(Optional.of(img));
        when(images.findImageData(img.getImageId())).thenReturn(PNG);
        OnlineExercise ex = OnlineExercise.builder().exerciseId(exId).space(space).status(OnlineExercise.Status.DRAFT).build();
        when(exercises.findByExerciseIdAndSpace_SpaceId(exId, space.getSpaceId())).thenReturn(Optional.of(ex));
        ExerciseImageServiceImpl service = service(R2FileStore.disabled());

        assertStatus(() -> service.content(learnerActor, space.getSpaceId(), img.getImageId()), HttpStatus.NOT_FOUND);
        ImageContent forTutor = service.content(tutorActor, space.getSpaceId(), img.getImageId());
        assertThat(forTutor.data()).isEqualTo(PNG);

        ex.setStatus(OnlineExercise.Status.PUBLISHED);
        ImageContent forLearner = service.content(learnerActor, space.getSpaceId(), img.getImageId());
        assertThat(forLearner.contentType()).isEqualTo("image/png");
    }

    @Test
    void oldUnusedImagesAreDeletedWhenUploading() {
        ExerciseImage old = ExerciseImage.builder().imageId(UUID.randomUUID()).spaceId(space.getSpaceId())
                .fileName("cu.png").fileSize(1L).contentType("image/png").build();
        when(images.findUnlinkedBefore(space.getSpaceId(), NOW.minusDays(1))).thenReturn(List.of(old));

        service(R2FileStore.disabled()).upload(tutorActor, space.getSpaceId(), new MockMultipartFile("file", "a.png", "image/png", PNG));

        verify(images).deleteAll(List.of(old));
    }
}
