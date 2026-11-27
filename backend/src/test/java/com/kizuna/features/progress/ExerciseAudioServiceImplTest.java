package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioInfo;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioUrl;
import com.kizuna.features.progress.entity.ExerciseAudio;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseAudioRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.ExerciseListenService;
import com.kizuna.features.progress.service.impl.ExerciseAudioServiceImpl;
import com.kizuna.features.storage.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** File nghe của câu hỏi bài làm trực tiếp lưu trên Cloudflare R2 (R2 giả lập): tải lên, quyền nghe, dọn file cũ. */
@SuppressWarnings("unchecked")
class ExerciseAudioServiceImplTest {

    private static final byte[] MP3 = {0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};

    private ClassroomService classrooms;
    private ExerciseAudioRepository audios;
    private OnlineExerciseRepository exercises;
    private R2StorageService r2;
    private S3Client s3;
    private ObjectProvider<R2StorageService> r2Provider;
    private ObjectProvider<S3Client> s3Provider;
    private ExerciseAudioServiceImpl service;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        audios = mock(ExerciseAudioRepository.class);
        exercises = mock(OnlineExerciseRepository.class);
        r2 = mock(R2StorageService.class);
        s3 = mock(S3Client.class);
        r2Provider = mock(ObjectProvider.class);
        s3Provider = mock(ObjectProvider.class);
        when(r2Provider.getIfAvailable()).thenReturn(r2);
        when(s3Provider.getIfAvailable()).thenReturn(s3);
        when(audios.save(any(ExerciseAudio.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ExerciseAudioServiceImpl(classrooms, audios, exercises, r2Provider, s3Provider,
                "kizunatutor-storage", 900, CLOCK);
        Tutor tutor = tutor();
        Learner learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        when(classrooms.loadManageableSpace(eq(tutorActor), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private MockMultipartFile mp3(String name) {
        return new MockMultipartFile("file", name, "audio/mpeg", MP3);
    }

    private ExerciseAudio stored(UUID exerciseId) {
        return ExerciseAudio.builder().audioId(UUID.randomUUID()).spaceId(space.getSpaceId()).exerciseId(exerciseId)
                .fileName("nghe.mp3").fileSize((long) MP3.length).contentType("audio/mpeg")
                .storageKey("audio/" + UUID.randomUUID() + ".mp3").build();
    }

    @Test
    void tutorUploadGoesToR2AndOnlyTheKeyIsSaved() {
        when(r2.uploadAudio(any())).thenReturn(new R2StorageService.UploadedAudio("audio/abc.mp3", "audio/mpeg", "https://r2/abc"));

        AudioInfo info = service.upload(tutorActor, space.getSpaceId(), mp3("../Bài nghe 1.mp3"));

        assertThat(info.fileName()).isEqualTo("Bài nghe 1.mp3");
        assertThat(info.fileSize()).isEqualTo(MP3.length);
        verify(audios).save(argThat(a -> "audio/abc.mp3".equals(a.getStorageKey()) && a.getExerciseId() == null
                && "audio/mpeg".equals(a.getContentType()) && space.getSpaceId().equals(a.getSpaceId())));
    }

    @Test
    void oldUnusedFilesAreDeletedOnR2AndInDbBeforeUploading() {
        ExerciseAudio old = stored(null);
        when(audios.findUnlinkedBefore(space.getSpaceId(), NOW.minusDays(1))).thenReturn(List.of(old));
        when(r2.uploadAudio(any())).thenReturn(new R2StorageService.UploadedAudio("audio/new.mp3", "audio/mpeg", "u"));

        service.upload(tutorActor, space.getSpaceId(), mp3("a.mp3"));

        verify(s3).deleteObject(argThat((DeleteObjectRequest r) ->
                "kizunatutor-storage".equals(r.bucket()) && old.getStorageKey().equals(r.key())));
        verify(audios).deleteAll(List.of(old));
    }

    @Test
    void learnerCannotGetADirectLinkForALimitedQuestionWhileDoingTheExercise() {
        UUID exId = UUID.randomUUID();
        ExerciseAudio audio = stored(exId);
        OnlineExercise ex = OnlineExercise.builder().exerciseId(exId).space(space).status(OnlineExercise.Status.PUBLISHED).build();
        when(audios.findByAudioIdAndSpaceId(audio.getAudioId(), space.getSpaceId())).thenReturn(Optional.of(audio));
        when(exercises.findByExerciseIdAndSpace_SpaceId(exId, space.getSpaceId())).thenReturn(Optional.of(ex));
        when(r2.presignedGetUrl(audio.getStorageKey())).thenReturn("https://r2/signed");
        ExerciseListenService listen = mock(ExerciseListenService.class);
        ExerciseAudioServiceImpl guarded = new ExerciseAudioServiceImpl(classrooms, audios, exercises, r2Provider, s3Provider,
                "kizunatutor-storage", 900, CLOCK, listen);

        when(listen.requiresCounting(any(), eq(exId), eq(audio.getAudioId()))).thenReturn(true);
        assertStatus(() -> guarded.url(learnerActor, space.getSpaceId(), audio.getAudioId()), HttpStatus.FORBIDDEN);
        // Gia sư vẫn nghe được; học viên đã nộp xong thì xem lại được
        assertThat(guarded.url(tutorActor, space.getSpaceId(), audio.getAudioId()).url()).isEqualTo("https://r2/signed");
        when(listen.requiresCounting(any(), eq(exId), eq(audio.getAudioId()))).thenReturn(false);
        assertThat(guarded.url(learnerActor, space.getSpaceId(), audio.getAudioId()).url()).isEqualTo("https://r2/signed");
    }

    @Test
    void oldFilesAreDeletedOnR2OnlyAfterTheTransactionCommits() {
        ExerciseAudio old = stored(null);
        when(audios.findUnlinkedBefore(space.getSpaceId(), NOW.minusDays(1))).thenReturn(List.of(old));
        when(r2.uploadAudio(any())).thenReturn(new R2StorageService.UploadedAudio("audio/new.mp3", "audio/mpeg", "u"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(tutorActor, space.getSpaceId(), mp3("a.mp3"));
            verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(s3).deleteObject(argThat((DeleteObjectRequest r) -> old.getStorageKey().equals(r.key())));
            verify(s3, never()).deleteObject(argThat((DeleteObjectRequest r) -> "audio/new.mp3".equals(r.key())));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void whenTheTransactionRollsBackTheJustUploadedFileIsRemovedAndOldFilesAreKept() {
        ExerciseAudio old = stored(null);
        when(audios.findUnlinkedBefore(space.getSpaceId(), NOW.minusDays(1))).thenReturn(List.of(old));
        when(r2.uploadAudio(any())).thenReturn(new R2StorageService.UploadedAudio("audio/new.mp3", "audio/mpeg", "u"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(tutorActor, space.getSpaceId(), mp3("a.mp3"));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(s3).deleteObject(argThat((DeleteObjectRequest r) -> "audio/new.mp3".equals(r.key())));
            verify(s3, never()).deleteObject(argThat((DeleteObjectRequest r) -> old.getStorageKey().equals(r.key())));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void fileThatIsNotReallyAudioIsRejectedBeforeUploading() {
        MockMultipartFile fake = new MockMultipartFile("file", "bai.mp3", "audio/mpeg", "<html>not audio</html>".getBytes());
        assertThatThrownBy(() -> service.upload(tutorActor, space.getSpaceId(), fake))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getFieldErrors()).containsKey("file");
                });
        verify(r2, never()).uploadAudio(any());
        verify(audios, never()).save(any());
    }

    @Test
    void audioFormatsAreDetectedFromFileHeader() {
        assertThat(ExerciseAudioServiceImpl.detectAudio(MP3, MP3.length)).isEqualTo("audio/mpeg");
        byte[] frame = {(byte) 0xFF, (byte) 0xFB, 0x50, 0x00};
        assertThat(ExerciseAudioServiceImpl.detectAudio(frame, frame.length)).isEqualTo("audio/mpeg");
        byte[] wav = "RIFF    WAVE".getBytes();
        assertThat(ExerciseAudioServiceImpl.detectAudio(wav, wav.length)).isEqualTo("audio/wav");
        byte[] ogg = "OggS  ".getBytes();
        assertThat(ExerciseAudioServiceImpl.detectAudio(ogg, ogg.length)).isEqualTo("audio/ogg");
        byte[] m4a = "    ftypM4A ".getBytes();
        assertThat(ExerciseAudioServiceImpl.detectAudio(m4a, m4a.length)).isEqualTo("audio/mp4");
        byte[] webm = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0x01};
        assertThat(ExerciseAudioServiceImpl.detectAudio(webm, webm.length)).isEqualTo("audio/webm");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0};
        assertThat(ExerciseAudioServiceImpl.detectAudio(png, png.length)).isNull();
    }

    @Test
    void uploadRulesAndMissingConfiguration() {
        // Lỗi định dạng / dung lượng từ R2StorageService → 400 ở trường file
        when(r2.uploadAudio(any())).thenThrow(new BadRequestException("Tệp âm thanh tối đa 25 MB."));
        assertThatThrownBy(() -> service.upload(tutorActor, space.getSpaceId(), mp3("dai.mp3")))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getFieldErrors()).containsEntry("file", "Tệp âm thanh tối đa 25 MB.");
                });
        // Học viên không được tải lên
        assertStatus(() -> service.upload(learnerActor, space.getSpaceId(), mp3("a.mp3")), HttpStatus.FORBIDDEN);
        // Máy chủ chưa bật R2
        when(r2Provider.getIfAvailable()).thenReturn(null);
        assertStatus(() -> service.upload(tutorActor, space.getSpaceId(), mp3("a.mp3")), HttpStatus.SERVICE_UNAVAILABLE);
        verify(audios, never()).save(any());
    }

    @Test
    void learnerGetsLinkOnlyForAudioOfAPublishedOpenExercise() {
        UUID exId = UUID.randomUUID();
        ExerciseAudio audio = stored(exId);
        OnlineExercise ex = OnlineExercise.builder().exerciseId(exId).space(space).status(OnlineExercise.Status.DRAFT).build();
        when(audios.findByAudioIdAndSpaceId(audio.getAudioId(), space.getSpaceId())).thenReturn(Optional.of(audio));
        when(exercises.findByExerciseIdAndSpace_SpaceId(exId, space.getSpaceId())).thenReturn(Optional.of(ex));
        when(r2.presignedGetUrl(audio.getStorageKey())).thenReturn("https://r2/signed");

        // Bài nháp: học viên không nghe được, gia sư nghe được
        assertStatus(() -> service.url(learnerActor, space.getSpaceId(), audio.getAudioId()), HttpStatus.NOT_FOUND);
        AudioUrl forTutor = service.url(tutorActor, space.getSpaceId(), audio.getAudioId());
        assertThat(forTutor.url()).isEqualTo("https://r2/signed");
        assertThat(forTutor.expiresInSeconds()).isEqualTo(900);

        // Đã giao nhưng chưa tới giờ mở
        ex.setStatus(OnlineExercise.Status.PUBLISHED);
        ex.setOpensAt(NOW.plusHours(1));
        assertStatus(() -> service.url(learnerActor, space.getSpaceId(), audio.getAudioId()), HttpStatus.NOT_FOUND);

        ex.setOpensAt(null);
        assertThat(service.url(learnerActor, space.getSpaceId(), audio.getAudioId()).url()).isEqualTo("https://r2/signed");

        // File chưa / không còn gắn với bài nào: học viên không nghe được
        ExerciseAudio loose = stored(null);
        when(audios.findByAudioIdAndSpaceId(loose.getAudioId(), space.getSpaceId())).thenReturn(Optional.of(loose));
        assertStatus(() -> service.url(learnerActor, space.getSpaceId(), loose.getAudioId()), HttpStatus.NOT_FOUND);
    }
}
