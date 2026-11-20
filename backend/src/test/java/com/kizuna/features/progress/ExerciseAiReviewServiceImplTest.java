package com.kizuna.features.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.progress.dto.ExerciseAiReviewResponse;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.AttemptRef;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.Candidate;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.NotifyTarget;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.ReviewRow;
import com.kizuna.features.progress.service.impl.ExerciseAiClient;
import com.kizuna.features.progress.service.impl.ExerciseAiReviewServiceImpl;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.service.impl.ExerciseAiReviewServiceImpl.DONE;
import static com.kizuna.features.progress.service.impl.ExerciseAiReviewServiceImpl.FAILED;
import static com.kizuna.features.progress.service.impl.ExerciseAiReviewServiceImpl.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** FE-15: AI gợi ý chấm câu tự luận — xử lý nền, quyền xem, chấm lại, thông báo. Không chạm DB. */
class ExerciseAiReviewServiceImplTest {

    private static final String MANUAL = "{\"accepted\":[],\"sample\":\"わたしは〜です。\"}";
    private static final String WITH_KEY = "{\"accepted\":[\"はい\"]}";
    private static final BigDecimal FIVE = new BigDecimal("5.00");

    private ExerciseAiReviewRepository repository;
    private ExerciseAiClient client;
    private ClassroomService classroomService;
    private NotificationEvents notifications;
    private ExerciseAiReviewServiceImpl service;

    private final UUID attemptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExerciseAiReviewRepository.class);
        client = mock(ExerciseAiClient.class);
        classroomService = mock(ClassroomService.class);
        notifications = mock(NotificationEvents.class);
        ObjectMapper mapper = new ObjectMapper();
        service = new ExerciseAiReviewServiceImpl(repository, client, new ExerciseQuestionEngine(mapper), mapper,
                classroomService, notifications, ProgressTestData.CLOCK);
        when(repository.claim(any(), any())).thenReturn(true);
    }

    private Candidate candidate(String type, String data, String response) {
        return candidate(type, data, response, null);
    }

    /** correct = kết quả máy chấm của FE-07 (null = máy chưa kết luận, câu chờ chấm). */
    private Candidate candidate(String type, String data, String response, Boolean correct) {
        return new Candidate(attemptId, UUID.randomUUID(), type, data, "Giới thiệu bản thân", FIVE, response,
                "Bài viết", null, correct);
    }

    private void queue(Candidate... items) {
        when(repository.findCandidates(any(OffsetDateTime.class), anyInt())).thenReturn(List.of(items));
    }

    @Nested
    class Processing {

        @Test
        void writtenQuestionWithoutAnswerKeyIsGradedByAi() {
            Candidate c = candidate("WRITTEN", MANUAL, "{\"text\":\"わたしはアンです。\"}");
            queue(c);
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply(
                    "{\"score\": 4.1, \"feedback\": \"Đúng ý, thiếu です ở câu 2.\"}", "GEMINI:gemini-2.5-flash"));

            assertThat(service.processPending()).isEqualTo(1);

            verify(repository).finish(eq(attemptId), eq(c.questionId()), eq(DONE),
                    argThat(s -> s.compareTo(new BigDecimal("4.00")) == 0), eq("Đúng ý, thiếu です ở câu 2."),
                    eq("GEMINI:gemini-2.5-flash"), isNull());
        }

        @Test
        void promptSentToAiContainsSampleAndLearnerAnswer() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"わたしはアンです。\"}"));
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\":5,\"feedback\":\"ok\"}", "m"));

            service.processPending();

            ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
            verify(client).complete(prompt.capture());
            assertThat(prompt.getValue()).contains("わたしは〜です。", "わたしはアンです。", "Giới thiệu bản thân");
        }

        @Test
        void choiceAndUnknownTypesAreSkipped() {
            queue(candidate("SINGLE_CHOICE", "{\"options\":[\"a\",\"b\"],\"correct\":0}", "{\"choice\":1}"),
                    candidate("UNKNOWN_TYPE", MANUAL, "{\"text\":\"x\"}"));

            assertThat(service.processPending()).isZero();

            verify(repository, never()).claim(any(), any());
            verifyNoInteractions(client);
        }

        @Test
        void answersFinalizedByMachineAreNotSentToAi() {
            // Theo FE-07 (Linh): điền khuyết khớp đáp án / bỏ trống hết và tự luận kiểu cũ có đáp án đều chốt điểm, không chờ chấm
            Candidate matched = candidate("FILL_BLANK", "{\"text\":\"6時に ___ います\",\"answers\":[[\"起きて\"]],\"review\":true}",
                    "{\"blanks\":[\"起きて\"]}", true);
            Candidate allEmpty = candidate("FILL_BLANK", "{\"text\":\"___\",\"answers\":[[\"a\"]],\"review\":true}",
                    "{\"blanks\":[\"\"]}", false);
            Candidate oldWritten = candidate("WRITTEN", WITH_KEY, "{\"text\":\"いいえ\"}", false);
            queue(matched, allEmpty, oldWritten);

            assertThat(service.processPending()).isZero();

            verify(repository, never()).claim(any(), any());
            verifyNoInteractions(client);
        }

        @Test
        void fillBlankNotMatchingKeyIsSentToAiWithTextBlanksAndAcceptedAnswers() {
            Candidate c = candidate("FILL_BLANK", "{\"text\":\"毎朝、6時に ___ います。\",\"answers\":[[\"起きて\",\"おきて\"]],\"review\":true}",
                    "{\"blanks\":[\"ために\"]}", null);
            queue(c);
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\": 0, \"feedback\": \"Sai từ.\"}", "m"));

            service.processPending();

            ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
            verify(client).complete(prompt.capture());
            assertThat(prompt.getValue()).contains("毎朝、6時に ___ います。", "chỗ 1: 起きて / おきて", "Chỗ 1: ために");
            verify(repository).finish(eq(attemptId), eq(c.questionId()), eq(DONE), argThat(s -> s.signum() == 0),
                    eq("Sai từ."), eq("m"), isNull());
        }

        @Test
        void fillBlankWithBlankWithoutAnswerIsSentToAiWithKnownAnswers() {
            // Ô 2 không có đáp án → câu luôn chờ chấm (needsManualGrading của FE-07)
            Candidate c = candidate("FILL_BLANK", "{\"text\":\"私は ___ を ___ ます\",\"answers\":[[\"りんご\"],[]],\"review\":true}",
                    "{\"blanks\":[\"りんご\",\"たべ\"]}", null);
            queue(c);
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\": 5, \"feedback\": \"Đúng.\"}", "m"));

            service.processPending();

            ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
            verify(client).complete(prompt.capture());
            assertThat(prompt.getValue()).contains("chỗ 1: りんご", "Chỗ 2: たべ");
        }

        @Test
        void fillBlankLeftEmptyGetsZeroWithoutAi() {
            Candidate c = candidate("FILL_BLANK", "{\"text\":\"___\",\"answers\":[[]],\"review\":true}", "{\"blanks\":[\"  \"]}", null);
            queue(c);

            service.processPending();

            verify(repository).finish(eq(attemptId), eq(c.questionId()), eq(DONE), argThat(s -> s.signum() == 0),
                    eq(ExerciseAiReviewServiceImpl.EMPTY_ANSWER_FEEDBACK), isNull(), isNull());
            verifyNoInteractions(client);
        }

        @Test
        void emptyAnswerGetsZeroWithoutCallingAi() {
            Candidate c = candidate("WRITTEN", MANUAL, "{\"text\":\"   \"}");
            Candidate none = candidate("WRITTEN", MANUAL, null);
            queue(c, none);

            service.processPending();

            verify(repository).finish(eq(attemptId), eq(c.questionId()), eq(DONE),
                    argThat(s -> s.signum() == 0), eq(ExerciseAiReviewServiceImpl.EMPTY_ANSWER_FEEDBACK), isNull(), isNull());
            verify(repository).finish(eq(attemptId), eq(none.questionId()), eq(DONE),
                    argThat(s -> s.signum() == 0), anyString(), isNull(), isNull());
            verifyNoInteractions(client);
        }

        @Test
        void questionClaimedByAnotherMachineIsNotSentToAi() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"abc\"}"));
            when(repository.claim(any(), any())).thenReturn(false);

            assertThat(service.processPending()).isZero();

            verifyNoInteractions(client);
            verify(repository, never()).finish(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void aiNotConfiguredOrDownMarksQuestionFailed() {
            Candidate c = candidate("WRITTEN", MANUAL, "{\"text\":\"abc\"}");
            queue(c);
            when(client.complete(anyString())).thenThrow(new ExerciseAiClient.AiUnavailableException("Chưa cấu hình khóa AI"));

            service.processPending();

            verify(repository).finish(attemptId, c.questionId(), FAILED, null, null, null, "Chưa cấu hình khóa AI");
        }

        @Test
        void outOfRangeScoreFromAiIsRejected() {
            Candidate c = candidate("WRITTEN", MANUAL, "{\"text\":\"abc\"}");
            queue(c);
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\": 9, \"feedback\": \"x\"}", "m"));

            service.processPending();

            verify(repository).finish(eq(attemptId), eq(c.questionId()), eq(FAILED), isNull(), isNull(), isNull(), anyString());
        }

        @Test
        void unexpectedErrorOnOneQuestionDoesNotStopOthers() {
            Candidate first = candidate("WRITTEN", MANUAL, "{\"text\":\"a\"}");
            Candidate second = candidate("WRITTEN", MANUAL, "{\"text\":\"b\"}");
            queue(first, second);
            when(client.complete(anyString()))
                    .thenThrow(new IllegalStateException("boom"))
                    .thenReturn(new ExerciseAiClient.Reply("{\"score\": 3, \"feedback\": \"ok\"}", "m"));

            assertThat(service.processPending()).isEqualTo(2);

            verify(repository).finish(eq(attemptId), eq(first.questionId()), eq(FAILED), isNull(), isNull(), isNull(), anyString());
            verify(repository).finish(eq(attemptId), eq(second.questionId()), eq(DONE), any(), eq("ok"), eq("m"), isNull());
        }

        @Test
        void aiNeverChangesOfficialScoresOrAttemptStatus() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"abc\"}"));
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\": 3, \"feedback\": \"ok\"}", "m"));

            service.processPending();

            // Chỉ ghi vào bảng gợi ý: repository FE-15 là chỗ ghi duy nhất, không có repository FE-07 nào được dùng
            verify(repository, atLeastOnce()).finish(any(), any(), any(), any(), any(), any(), any());
            verifyNoInteractions(classroomService);
        }

        @Test
        void missingTableErrorReachesTheWorker() {
            when(repository.findCandidates(any(OffsetDateTime.class), anyInt()))
                    .thenThrow(new DataAccessResourceFailureException("relation does not exist"));

            assertThatThrownBy(() -> service.processPending()).isInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    @Nested
    class Notification {

        private final UUID tutorUserId = UUID.randomUUID();
        private final UUID spaceId = UUID.randomUUID();
        private final UUID exerciseId = UUID.randomUUID();

        @BeforeEach
        void target() {
            when(repository.findNotifyTarget(attemptId)).thenReturn(Optional.of(
                    new NotifyTarget(tutorUserId, "Nguyễn An", "Bài viết", spaceId, exerciseId)));
            when(client.complete(anyString())).thenReturn(new ExerciseAiClient.Reply("{\"score\": 3, \"feedback\": \"ok\"}", "m"));
        }

        private ReviewRow row(String status) {
            return new ReviewRow(UUID.randomUUID(), status, null, null, ProgressTestData.NOW);
        }

        @Test
        void tutorIsNotifiedOnceWhenAllQuestionsOfAttemptAreDone() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"a\"}"), candidate("WRITTEN", MANUAL, "{\"text\":\"b\"}"));
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(row(DONE), row(DONE)));

            service.processPending();

            ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> path = ArgumentCaptor.forClass(String.class);
            verify(notifications, times(1)).publish(eq("exercise-ai:" + attemptId), eq("ASSIGNMENT"),
                    eq(ExerciseAiReviewServiceImpl.NOTIFY_TITLE), content.capture(), path.capture(), eq(List.of(tutorUserId)));
            assertThat(content.getValue()).contains("Bài viết", "Nguyễn An", "2 câu");
            assertThat(path.getValue()).isEqualTo("/pages/exercise-editor/index.html?space=" + spaceId
                    + "&exercise=" + exerciseId + "&tab=bai-lam");
            assertThat(NotificationEvents.safePath(path.getValue())).isTrue();
        }

        @Test
        void failedQuestionsAreMentionedInNotification() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"a\"}"));
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(row(DONE), row(FAILED)));

            service.processPending();

            ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
            verify(notifications).publish(anyString(), anyString(), anyString(), content.capture(), anyString(), any());
            assertThat(content.getValue()).contains("1 câu AI chưa chấm được");
        }

        @Test
        void noNotificationWhileAQuestionIsStillPendingOrNotYetClaimed() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"a\"}"));
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(row(DONE), row(PENDING)));

            service.processPending();
            verifyNoInteractions(notifications);

            when(repository.findByAttempt(attemptId)).thenReturn(List.of(row(DONE)));
            when(repository.findCandidates(eq(attemptId), any(OffsetDateTime.class)))
                    .thenReturn(List.of(candidate("WRITTEN", MANUAL, "{\"text\":\"c\"}")));
            service.processPending();
            verifyNoInteractions(notifications);
        }

        @Test
        void notificationErrorDoesNotFailGrading() {
            queue(candidate("WRITTEN", MANUAL, "{\"text\":\"a\"}"));
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(row(DONE)));
            doThrow(new IllegalArgumentException("bad")).when(notifications)
                    .publish(anyString(), anyString(), anyString(), anyString(), anyString(), any());

            assertThat(service.processPending()).isEqualTo(1);
        }
    }

    @Nested
    class TutorAccess {

        private Tutor tutor;
        private Learner learner;
        private LearningSpace space;
        private final UUID exerciseId = UUID.randomUUID();

        @BeforeEach
        void classroom() {
            tutor = ProgressTestData.tutor();
            learner = ProgressTestData.learner();
            space = ProgressTestData.space(tutor, learner);
            when(classroomService.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
            when(classroomService.loadGradableSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
            when(repository.findAttempt(space.getSpaceId(), exerciseId, attemptId))
                    .thenReturn(Optional.of(new AttemptRef(attemptId, "SUBMITTED")));
        }

        @Test
        void tutorOfClassSeesSuggestionsWithVietnameseLabels() {
            UUID q1 = UUID.randomUUID();
            UUID q2 = UUID.randomUUID();
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(
                    new ReviewRow(q1, DONE, new BigDecimal("3.50"), "Tốt", ProgressTestData.NOW),
                    new ReviewRow(q2, FAILED, null, null, ProgressTestData.NOW)));

            List<ExerciseAiReviewResponse> list = service.list(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId);

            assertThat(list).extracting(ExerciseAiReviewResponse::statusLabel).containsExactly("AI đã gợi ý", "AI chưa chấm được");
            assertThat(list.get(0).suggestedScore()).isEqualByComparingTo("3.5");
            assertThat(list.get(1).suggestedScore()).isNull();
        }

        @Test
        void pendingForMoreThan10MinutesIsShownAsFailed() {
            when(repository.findByAttempt(attemptId)).thenReturn(List.of(
                    new ReviewRow(UUID.randomUUID(), PENDING, null, null, ProgressTestData.NOW.minusMinutes(11)),
                    new ReviewRow(UUID.randomUUID(), PENDING, null, null, ProgressTestData.NOW.minusMinutes(9))));

            List<ExerciseAiReviewResponse> list = service.list(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId);

            assertThat(list).extracting(ExerciseAiReviewResponse::status).containsExactly(FAILED, PENDING);
        }

        @Test
        void learnerAndOtherTutorCannotSeeSuggestions() {
            assertThatThrownBy(() -> service.list(ProgressTestData.asLearner(learner), space.getSpaceId(), exerciseId, attemptId))
                    .isInstanceOf(ClassroomException.class)
                    .hasMessage(ExerciseAiReviewServiceImpl.MSG_FORBIDDEN);
            assertThatThrownBy(() -> service.list(ProgressTestData.asTutor(ProgressTestData.tutor()), space.getSpaceId(), exerciseId, attemptId))
                    .isInstanceOf(ClassroomException.class)
                    .hasMessage(ExerciseAiReviewServiceImpl.MSG_FORBIDDEN);
            verify(repository, never()).findByAttempt(any());
        }

        @Test
        void attemptOfAnotherExerciseIsNotFound() {
            UUID other = UUID.randomUUID();

            assertThatThrownBy(() -> service.list(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, other))
                    .isInstanceOf(ClassroomException.class)
                    .hasMessage(ExerciseAiReviewServiceImpl.MSG_NOT_FOUND);
        }

        @Test
        void missingTableGivesFriendlyUnavailableError() {
            when(repository.findAttempt(any(), any(), any())).thenThrow(new DataAccessResourceFailureException("x"));

            assertThatThrownBy(() -> service.list(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId))
                    .isInstanceOfSatisfying(ClassroomException.class, e -> {
                        assertThat(e.getMessage()).isEqualTo(ExerciseAiReviewServiceImpl.MSG_UNAVAILABLE);
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    });
        }

        @Test
        void retryDeletesFailedAndStuckSuggestionsSoWorkerGradesAgain() {
            service.retry(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId);

            verify(classroomService).loadGradableSpace(any(), eq(space.getSpaceId()));
            verify(repository).deleteRetryable(attemptId, ProgressTestData.NOW.minusMinutes(10));
        }

        @Test
        void retryFollowsGradingPermissionOfClassroom() {
            when(classroomService.loadGradableSpace(any(), any()))
                    .thenThrow(ClassroomException.conflict("Lớp học không còn hoạt động nên không thể chấm điểm."));

            assertThatThrownBy(() -> service.retry(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId))
                    .isInstanceOf(ClassroomException.class);
            verify(repository, never()).deleteRetryable(any(), any());
        }

        @Test
        void retryOnAttemptInProgressIsRejected() {
            when(repository.findAttempt(space.getSpaceId(), exerciseId, attemptId))
                    .thenReturn(Optional.of(new AttemptRef(attemptId, "IN_PROGRESS")));

            assertThatThrownBy(() -> service.retry(ProgressTestData.asTutor(tutor), space.getSpaceId(), exerciseId, attemptId))
                    .isInstanceOf(ClassroomException.class)
                    .hasMessage(ExerciseAiReviewServiceImpl.MSG_NOT_SUBMITTED);
            verify(repository, never()).deleteRetryable(any(), any());
        }
    }
}
