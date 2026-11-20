package com.kizuna.features.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenGrant;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenStatus;
import com.kizuna.features.progress.entity.ExerciseAttempt;
import com.kizuna.features.progress.entity.ExerciseAudio;
import com.kizuna.features.progress.entity.ExerciseQuestion;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseAttemptRepository;
import com.kizuna.features.progress.repository.ExerciseAudioRepository;
import com.kizuna.features.progress.repository.ExerciseListenCountRepository;
import com.kizuna.features.progress.repository.ExerciseQuestionRepository;
import com.kizuna.features.progress.service.impl.ExerciseListenServiceImpl;
import com.kizuna.features.storage.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Số lần nghe đếm ở máy chủ: mỗi lần bấm nghe tính 1 lần, hết lượt / hết giờ / đã nộp thì không cấp link. */
@SuppressWarnings("unchecked")
class ExerciseListenServiceImplTest {

    private ClassroomService classrooms;
    private ExerciseAttemptRepository attempts;
    private ExerciseQuestionRepository questions;
    private ExerciseAudioRepository audios;
    private ExerciseListenCountRepository counts;
    private R2StorageService r2;
    private ExerciseListenServiceImpl service;
    private LearningSpace space;
    private Learner learner;
    private CurrentActor learnerActor;
    private CurrentActor tutorActor;
    private OnlineExercise exercise;
    private ExerciseAttempt attempt;
    private ExerciseQuestion limited;
    private ExerciseQuestion unlimited;
    private ExerciseAudio audio;

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        attempts = mock(ExerciseAttemptRepository.class);
        questions = mock(ExerciseQuestionRepository.class);
        audios = mock(ExerciseAudioRepository.class);
        counts = mock(ExerciseListenCountRepository.class);
        r2 = mock(R2StorageService.class);
        ObjectProvider<R2StorageService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(r2);
        service = new ExerciseListenServiceImpl(classrooms, attempts, questions, audios, counts, provider,
                new ObjectMapper(), 900, CLOCK);

        Tutor tutor = tutor();
        learner = learner();
        space = space(tutor, learner);
        learnerActor = asLearner(learner);
        tutorActor = asTutor(tutor);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);

        exercise = OnlineExercise.builder().exerciseId(UUID.randomUUID()).space(space)
                .status(OnlineExercise.Status.PUBLISHED).build();
        attempt = ExerciseAttempt.builder().attemptId(UUID.randomUUID()).exercise(exercise).learner(learner)
                .attemptNumber(1).startedAt(NOW.minusMinutes(5)).deadlineAt(NOW.plusMinutes(10))
                .status(ExerciseAttempt.Status.IN_PROGRESS).maxScore(BigDecimal.TEN).build();
        when(attempts.findById(attempt.getAttemptId())).thenReturn(Optional.of(attempt));
        when(attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exercise.getExerciseId())).thenReturn(List.of(attempt));

        audio = ExerciseAudio.builder().audioId(UUID.randomUUID()).spaceId(space.getSpaceId()).exerciseId(exercise.getExerciseId())
                .fileName("nghe.mp3").fileSize(10L).contentType("audio/mpeg").storageKey("audio/x.mp3").build();
        when(audios.findByAudioIdAndSpaceId(audio.getAudioId(), space.getSpaceId())).thenReturn(Optional.of(audio));
        when(r2.presignedGetUrl("audio/x.mp3")).thenReturn("https://r2/signed");

        limited = question(1, "{\"correct\":true,\"audio\":{\"id\":\"" + audio.getAudioId() + "\",\"maxPlays\":2}}");
        unlimited = question(2, "{\"correct\":true,\"audio\":{\"id\":\"" + audio.getAudioId() + "\"}}");
        when(questions.findByExercise_ExerciseIdOrderByPositionAsc(exercise.getExerciseId())).thenReturn(List.of(limited, unlimited));
    }

    private ExerciseQuestion question(int position, String data) {
        return ExerciseQuestion.builder().questionId(UUID.randomUUID()).exercise(exercise).position(position)
                .type(ExerciseQuestionType.TRUE_FALSE).prompt("Nghe").data(data).points(BigDecimal.ONE).build();
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void eachListenIsCountedOnTheServerAndStopsAtTheLimit() {
        when(counts.countPlay(attempt.getAttemptId(), limited.getQuestionId(), 2, NOW)).thenReturn(1, 2, null);

        ListenGrant first = service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false);
        assertThat(first.url()).isEqualTo("https://r2/signed");
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false)
                .remaining()).isZero();
        // Hết lượt: không cấp link nữa (đổi máy / trình duyệt cũng vậy vì máy chủ đếm)
        assertThatThrownBy(() -> service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("nghe hết 2 lần");
                });
    }

    @Test
    void resumingAnExpiredLinkDoesNotCountAnotherPlay() {
        when(counts.resume(eq(attempt.getAttemptId()), eq(limited.getQuestionId()), eq(NOW.minusMinutes(30)), eq(2))).thenReturn(true, false);
        when(counts.playsOf(attempt.getAttemptId())).thenReturn(Map.of(limited.getQuestionId(), 1));

        ListenGrant resumed = service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), true);
        assertThat(resumed.plays()).isEqualTo(1);
        assertThat(resumed.remaining()).isEqualTo(1);
        verify(counts, never()).countPlay(any(), any(), anyInt(), any());
        // Quá số lần xin lại / quá 30 phút → phải bấm nghe lại (tính thêm lần)
        assertStatus(() -> service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), true),
                HttpStatus.CONFLICT);
    }

    @Test
    void unlimitedQuestionGetsALinkWithoutCounting() {
        ListenGrant g = service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), unlimited.getQuestionId(), false);
        assertThat(g.maxPlays()).isNull();
        assertThat(g.remaining()).isNull();
        verify(counts, never()).countPlay(any(), any(), anyInt(), any());
    }

    @Test
    void noLinkAfterSubmittingOrAfterTimeIsUpAndOnlyForOwnAttempt() {
        // Hết giờ (quá thời gian ân hạn)
        attempt.setDeadlineAt(NOW.minusMinutes(1));
        assertStatus(() -> service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false),
                HttpStatus.CONFLICT);
        // Đã nộp
        attempt.setDeadlineAt(NOW.plusMinutes(10));
        attempt.setStatus(ExerciseAttempt.Status.SUBMITTED);
        assertStatus(() -> service.listen(learnerActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false),
                HttpStatus.CONFLICT);
        // Gia sư không dùng API đếm lần nghe; lượt không có thì báo không tìm thấy
        assertStatus(() -> service.listen(tutorActor, space.getSpaceId(), attempt.getAttemptId(), limited.getQuestionId(), false),
                HttpStatus.FORBIDDEN);
        assertStatus(() -> service.listen(learnerActor, space.getSpaceId(), UUID.randomUUID(), limited.getQuestionId(), false),
                HttpStatus.NOT_FOUND);
        verify(counts, never()).countPlay(any(), any(), anyInt(), any());
    }

    @Test
    void statusListsPlaysOfEachListeningQuestion() {
        when(counts.playsOf(attempt.getAttemptId())).thenReturn(Map.of(limited.getQuestionId(), 2));

        List<ListenStatus> list = service.status(learnerActor, space.getSpaceId(), attempt.getAttemptId());

        assertThat(list).hasSize(2);
        assertThat(list.get(0).plays()).isEqualTo(2);
        assertThat(list.get(0).remaining()).isZero();
        assertThat(list.get(1).maxPlays()).isNull();
    }

    @Test
    void directLinkIsBlockedWhileTheLearnerIsStillDoingTheExercise() {
        UUID learnerId = learner.getLearnerId();
        // Đang làm bài → phải nghe qua API đếm
        assertThat(service.requiresCounting(learnerId, exercise.getExerciseId(), audio.getAudioId())).isTrue();
        // Đã nộp, không còn lượt đang làm → xem lại thoải mái
        attempt.setStatus(ExerciseAttempt.Status.SUBMITTED);
        assertThat(service.requiresCounting(learnerId, exercise.getExerciseId(), audio.getAudioId())).isFalse();
        // Chưa làm lượt nào → chưa được nghe trước
        when(attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exercise.getExerciseId())).thenReturn(List.of());
        assertThat(service.requiresCounting(learnerId, exercise.getExerciseId(), audio.getAudioId())).isTrue();
        // File chỉ dùng ở câu không giới hạn → không chặn
        when(questions.findByExercise_ExerciseIdOrderByPositionAsc(exercise.getExerciseId())).thenReturn(List.of(unlimited));
        assertThat(service.requiresCounting(learnerId, exercise.getExerciseId(), audio.getAudioId())).isFalse();
    }
}
