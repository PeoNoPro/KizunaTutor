package com.kizuna.features.progress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.progress.repository.ExerciseAnswerRepository;
import com.kizuna.features.progress.repository.ExerciseAttemptRepository;
import com.kizuna.features.progress.repository.ExerciseQuestionRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.impl.OnlineExerciseServiceImpl;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Bài làm trực tiếp: soạn đề, học viên làm, tự nộp khi hết giờ, chấm tự động / gia sư chấm, nhiều lượt.
 * Repository giả lập bằng danh sách trong bộ nhớ. "Bây giờ" có thể dời bằng {@link #clockAt}.
 */
class OnlineExerciseServiceImplTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private ClassroomService classrooms;
    private OnlineExerciseRepository exercises;
    private ExerciseQuestionRepository questions;
    private ExerciseAttemptRepository attempts;
    private ExerciseAnswerRepository answers;
    private LearningSpaceRepository spaces;
    private OffsetDateTime now = NOW;
    private OnlineExerciseServiceImpl service;

    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;
    private Learner learner;

    private final List<OnlineExercise> exStore = new ArrayList<>();
    private final List<ExerciseQuestion> qStore = new ArrayList<>();
    private final List<ExerciseAttempt> aStore = new ArrayList<>();
    private final List<ExerciseAnswer> ansStore = new ArrayList<>();

    @BeforeEach
    void setUp() {
        classrooms = mock(ClassroomService.class);
        exercises = mock(OnlineExerciseRepository.class);
        questions = mock(ExerciseQuestionRepository.class);
        attempts = mock(ExerciseAttemptRepository.class);
        answers = mock(ExerciseAnswerRepository.class);
        spaces = mock(LearningSpaceRepository.class);
        build();

        Tutor tutor = tutor();
        learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(eq(tutorActor), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(eq(learnerActor), eq(space.getSpaceId())))
                .thenThrow(ClassroomException.forbidden("Chỉ gia sư"));

        when(exercises.save(any(OnlineExercise.class))).thenAnswer(inv -> {
            OnlineExercise e = inv.getArgument(0);
            if (e.getExerciseId() == null) {
                e.setExerciseId(UUID.randomUUID());
                e.setCreatedAt(now);
                exStore.add(e);
            }
            return e;
        });
        when(exercises.findBySpace_SpaceIdOrderByCreatedAtDesc(any())).thenAnswer(inv -> List.copyOf(exStore));
        when(exercises.findByExerciseIdAndSpace_SpaceId(any(), any())).thenAnswer(inv -> exStore.stream()
                .filter(e -> e.getExerciseId().equals(inv.getArgument(0))).findFirst());
        doAnswer(inv -> exStore.remove(inv.<OnlineExercise>getArgument(0))).when(exercises).delete(any(OnlineExercise.class));

        when(questions.saveAll(any())).thenAnswer(inv -> {
            List<ExerciseQuestion> list = new ArrayList<>();
            inv.<Iterable<ExerciseQuestion>>getArgument(0).forEach(q -> {
                q.setQuestionId(UUID.randomUUID());
                qStore.add(q);
                list.add(q);
            });
            return list;
        });
        when(questions.findByExercise_ExerciseIdOrderByPositionAsc(any())).thenAnswer(inv -> qStore.stream()
                .filter(q -> q.getExercise().getExerciseId().equals(inv.getArgument(0)))
                .sorted(Comparator.comparing(ExerciseQuestion::getPosition)).toList());
        when(questions.findByExercise_ExerciseIdIn(any())).thenAnswer(inv -> List.copyOf(qStore));
        when(questions.deleteByExercise_ExerciseId(any())).thenAnswer(inv -> {
            qStore.removeIf(q -> q.getExercise().getExerciseId().equals(inv.getArgument(0)));
            return 1L;
        });

        when(attempts.save(any(ExerciseAttempt.class))).thenAnswer(inv -> {
            ExerciseAttempt a = inv.getArgument(0);
            if (a.getAttemptId() == null) {
                a.setAttemptId(UUID.randomUUID());
                aStore.add(a);
            }
            return a;
        });
        when(attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(any())).thenAnswer(inv -> aStore.stream()
                .filter(a -> a.getExercise().getExerciseId().equals(inv.getArgument(0))).toList());
        when(attempts.findByExercise_ExerciseIdInOrderByAttemptNumberAsc(any())).thenAnswer(inv -> List.copyOf(aStore));
        when(attempts.findByAttemptIdAndExercise_ExerciseId(any(), any())).thenAnswer(inv -> aStore.stream()
                .filter(a -> a.getAttemptId().equals(inv.getArgument(0))).findFirst());
        when(attempts.existsByExercise_ExerciseId(any())).thenAnswer(inv -> aStore.stream()
                .anyMatch(a -> a.getExercise().getExerciseId().equals(inv.getArgument(0))));

        when(answers.save(any(ExerciseAnswer.class))).thenAnswer(inv -> {
            ExerciseAnswer x = inv.getArgument(0);
            if (x.getAnswerId() == null) {
                x.setAnswerId(UUID.randomUUID());
                ansStore.add(x);
            }
            return x;
        });
        when(answers.findByAttempt_AttemptId(any())).thenAnswer(inv -> ansStore.stream()
                .filter(x -> x.getAttempt().getAttemptId().equals(inv.getArgument(0))).toList());
        when(answers.findByAttempt_AttemptIdAndQuestionId(any(), any())).thenAnswer(inv -> ansStore.stream()
                .filter(x -> x.getAttempt().getAttemptId().equals(inv.getArgument(0))
                        && x.getQuestionId().equals(inv.getArgument(1))).findFirst());
    }

    private void build() {
        service = new OnlineExerciseServiceImpl(classrooms, exercises, questions, attempts, answers, spaces,
                new ExerciseQuestionEngine(mapper), Clock.fixed(now.toInstant(), now.getOffset()));
    }

    private void clockAt(OffsetDateTime t) {
        now = t;
        build();
    }

    private JsonNode json(String s) {
        try {
            return mapper.readTree(s.replace('\'', '"'));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private QuestionInput q(ExerciseQuestionType type, String prompt, String points, String data) {
        return new QuestionInput(type, prompt, new BigDecimal(points), json(data));
    }

    private List<QuestionInput> autoQuestions() {
        return List.of(
                q(ExerciseQuestionType.SINGLE_CHOICE, "「本」の読み方は？", "2", "{'options':['ほん','き','ひと'],'correct':0}"),
                q(ExerciseQuestionType.FILL_BLANK, null, "3", "{'text':'私は りんごを ___。','answers':[['たべます','食べます']]}"),
                q(ExerciseQuestionType.MATCHING, "Nối", "5", "{'pairs':[{'left':'犬','right':'dog'},{'left':'猫','right':'cat'}]}"));
    }

    private ExerciseRequest request(String mode, Integer minutes, int maxAttempts, boolean show, boolean publish,
                                    List<QuestionInput> qs) {
        return new ExerciseRequest("Từ vựng bài 5", "Làm cẩn thận", null, NOW.plusDays(3), minutes, maxAttempts, mode,
                show, publish, qs);
    }

    private UUID publishAuto(Integer minutes, int maxAttempts, boolean show) {
        return service.create(tutorActor, space.getSpaceId(), request("AUTO", minutes, maxAttempts, show, true, autoQuestions()))
                .summary().exerciseId();
    }

    private void answerAllCorrect(UUID ex, AttemptView a) {
        for (AttemptQuestion item : a.questions()) {
            String r = switch (item.type()) {
                case SINGLE_CHOICE -> "{'choice':0}";
                case FILL_BLANK -> "{'blanks':['食べます']}";
                case MATCHING -> "{'matches':['dog','cat']}";
                default -> "{}";
            };
            service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), item.questionId(), json(r));
        }
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    // ---------------------------------------------------------------- soạn đề

    @Test
    void draftIsHiddenFromLearnerAndCanBeIncomplete() {
        ExerciseDetail d = service.create(tutorActor, space.getSpaceId(), request("AUTO", null, 1, true, false,
                List.of(q(ExerciseQuestionType.SINGLE_CHOICE, "", "1", "{'options':['a']}"))));
        assertThat(d.summary().state()).isEqualTo("DRAFT");
        assertThat(service.list(learnerActor, space.getSpaceId())).isEmpty();
        assertStatus(() -> service.detail(learnerActor, space.getSpaceId(), d.summary().exerciseId()), HttpStatus.NOT_FOUND);
        // Giao bài khi câu hỏi chưa đủ → báo đúng câu
        assertThatThrownBy(() -> service.update(tutorActor, space.getSpaceId(), d.summary().exerciseId(),
                request("AUTO", null, 1, true, true, List.of(q(ExerciseQuestionType.SINGLE_CHOICE, "Đề", "1", "{'options':['a','b']}")))))
                .hasMessage("Câu 1: chưa chọn đáp án đúng.");
    }

    @Test
    void autoModeRequiresAnswersForEveryQuestionAndTutorModeAllowsEssay() {
        List<QuestionInput> withEssay = List.of(q(ExerciseQuestionType.WRITTEN, "Viết 3 câu", "5", "{}"));
        assertThatThrownBy(() -> service.create(tutorActor, space.getSpaceId(), request("AUTO", null, 1, true, true, withEssay)))
                .hasMessageContaining("Gia sư chấm");
        assertThat(service.create(tutorActor, space.getSpaceId(), request("TUTOR", null, 1, true, true, withEssay))
                .summary().gradingMode()).isEqualTo("TUTOR");
    }

    @Test
    void notOpenYetIsHiddenFromLearner() {
        service.create(tutorActor, space.getSpaceId(), new ExerciseRequest("Ngày mai", null, NOW.plusDays(1), null, null, 1,
                "AUTO", true, true, autoQuestions()));
        assertThat(service.list(tutorActor, space.getSpaceId())).singleElement()
                .extracting(ExerciseSummary::state).isEqualTo("NOT_OPEN");
        assertThat(service.list(learnerActor, space.getSpaceId())).isEmpty();
        clockAt(NOW.plusDays(1).plusMinutes(1));
        assertThat(service.list(learnerActor, space.getSpaceId())).singleElement()
                .extracting(ExerciseSummary::canStart).isEqualTo(true);
    }

    // ---------------------------------------------------------------- làm bài

    @Test
    void learnerDoesNotReceiveAnswersWhileDoingAndGetsScoreRightAfterSubmit() {
        UUID ex = publishAuto(30, 1, true);
        AttemptView a = service.start(learnerActor, space.getSpaceId(), ex);
        assertThat(a.status()).isEqualTo("IN_PROGRESS");
        assertThat(a.remainingSeconds()).isEqualTo(30 * 60L);
        assertThat(a.questions()).allSatisfy(item -> {
            assertThat(item.correctAnswer()).isNull();
            assertThat(item.view().toString()).doesNotContain("correct").doesNotContain("answers");
        });

        answerAllCorrect(ex, a);
        // Chỉ sai câu nối cột 1 cặp → cả câu 0 điểm
        UUID matching = a.questions().get(2).questionId();
        service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), matching, json("{'matches':['cat','dog']}"));

        AttemptView done = service.submit(learnerActor, space.getSpaceId(), ex, a.attemptId());
        assertThat(done.status()).isEqualTo("GRADED");
        assertThat(done.finalScore()).isEqualByComparingTo("5");
        assertThat(done.maxScore()).isEqualByComparingTo("10");
        assertThat(done.answersVisible()).isTrue();
        assertThat(done.questions().get(2).correct()).isFalse();
        assertThat(done.questions().get(0).correctAnswer().get("correct").asInt()).isEqualTo(0);

        // Nộp rồi không sửa được
        assertStatus(() -> service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), matching,
                json("{'matches':['dog','cat']}")), HttpStatus.CONFLICT);
    }

    @Test
    void closingThePageResumesTheSameAttemptWithSavedAnswersAndRemainingTime() {
        UUID ex = publishAuto(30, 1, true);
        AttemptView a = service.start(learnerActor, space.getSpaceId(), ex);
        service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), a.questions().get(0).questionId(), json("{'choice':0}"));

        clockAt(NOW.plusMinutes(12));
        AttemptView again = service.start(learnerActor, space.getSpaceId(), ex);
        assertThat(again.attemptId()).isEqualTo(a.attemptId());
        assertThat(again.attemptNumber()).isEqualTo(1);
        assertThat(again.remainingSeconds()).isEqualTo(18 * 60L);
        assertThat(again.questions().get(0).response().get("choice").asInt()).isZero();
        assertThat(aStore).hasSize(1);
    }

    @Test
    void timeoutAutoSubmitsWithSavedAnswersAndCountsTheAttempt() {
        UUID ex = publishAuto(30, 1, true);
        AttemptView a = service.start(learnerActor, space.getSpaceId(), ex);
        answerAllCorrect(ex, a);

        clockAt(NOW.plusHours(2));
        ExerciseSummary s = service.list(learnerActor, space.getSpaceId()).get(0);
        assertThat(s.attemptsUsed()).isEqualTo(1);
        assertThat(s.inProgressAttemptId()).isNull();
        assertThat(s.bestScore()).isEqualByComparingTo("10");
        assertThat(s.canStart()).isFalse();
        AttemptView done = service.attempt(learnerActor, space.getSpaceId(), ex, a.attemptId());
        assertThat(done.submitReason()).isEqualTo("TIMEOUT");
        assertThat(done.submittedAt()).isEqualTo(NOW.plusMinutes(30));
        assertStatus(() -> service.start(learnerActor, space.getSpaceId(), ex), HttpStatus.CONFLICT);
    }

    @Test
    void timeLimitIsKeptEvenIfCloseTimeComesFirstAndSubmissionIsLate() {
        ExerciseDetail d = service.create(tutorActor, space.getSpaceId(), new ExerciseRequest("Bài", null, null,
                NOW.plusMinutes(10), 30, 1, "AUTO", true, true, autoQuestions()));
        UUID ex = d.summary().exerciseId();
        AttemptView a = service.start(learnerActor, space.getSpaceId(), ex);
        clockAt(NOW.plusMinutes(20));
        AttemptView done = service.submit(learnerActor, space.getSpaceId(), ex, a.attemptId());
        assertThat(done.status()).isEqualTo("GRADED");
        assertThat(done.late()).isTrue();
        // Sau giờ đóng đề không bắt đầu lượt mới được
        assertStatus(() -> service.start(learnerActor, space.getSpaceId(), ex), HttpStatus.CONFLICT);
    }

    @Test
    void multipleAttemptsTakeBestScoreAndHideAnswersWhenTutorTurnsThemOff() {
        UUID ex = publishAuto(null, 2, false);
        AttemptView first = service.start(learnerActor, space.getSpaceId(), ex);
        answerAllCorrect(ex, first);
        AttemptView r1 = service.submit(learnerActor, space.getSpaceId(), ex, first.attemptId());
        assertThat(r1.answersVisible()).isFalse();
        assertThat(r1.questions()).allSatisfy(item -> assertThat(item.correctAnswer()).isNull());
        assertThat(r1.questions().get(0).correct()).isTrue();

        AttemptView second = service.start(learnerActor, space.getSpaceId(), ex);
        assertThat(second.attemptNumber()).isEqualTo(2);
        service.submit(learnerActor, space.getSpaceId(), ex, second.attemptId()); // bỏ trống → 0 điểm
        ExerciseSummary s = service.list(learnerActor, space.getSpaceId()).get(0);
        assertThat(s.bestScore()).isEqualByComparingTo("10");
        assertThat(s.canStart()).isFalse();
        assertStatus(() -> service.start(learnerActor, space.getSpaceId(), ex), HttpStatus.CONFLICT);

        // Gia sư cho thêm 1 lượt bằng cách tăng số lượt; không được giảm dưới số lượt đã dùng
        assertThatThrownBy(() -> service.update(tutorActor, space.getSpaceId(), ex, request("AUTO", null, 1, false, true, autoQuestions())))
                .hasMessageContaining("2 trở lên");
        service.update(tutorActor, space.getSpaceId(), ex, request("AUTO", null, 3, true, true, autoQuestions()));
        assertThat(service.start(learnerActor, space.getSpaceId(), ex).attemptNumber()).isEqualTo(3);
        // Bật hiện đáp án → lượt cũ cũng xem được đáp án
        assertThat(service.attempt(learnerActor, space.getSpaceId(), ex, first.attemptId()).answersVisible()).isTrue();
    }

    @Test
    void questionsAreLockedOnceLearnerStarted() {
        UUID ex = publishAuto(null, 1, true);
        service.start(learnerActor, space.getSpaceId(), ex);
        List<QuestionInput> changed = List.of(q(ExerciseQuestionType.TRUE_FALSE, "Mới", "1", "{'correct':true}"));
        ExerciseDetail d = service.update(tutorActor, space.getSpaceId(), ex, request("AUTO", null, 1, true, true, changed));
        assertThat(d.questions()).hasSize(3);
        assertThat(d.summary().locked()).isTrue();
        assertStatus(() -> service.delete(tutorActor, space.getSpaceId(), ex), HttpStatus.CONFLICT);
    }

    // ---------------------------------------------------------------- gia sư chấm

    @Test
    void essayWaitsForTutorThenTutorCanAdjustAnyScore() {
        List<QuestionInput> qs = List.of(
                q(ExerciseQuestionType.TRUE_FALSE, "犬は動物です", "2", "{'correct':true}"),
                q(ExerciseQuestionType.WRITTEN, "Giới thiệu bản thân", "8", "{'sample':'わたしは…'}"));
        UUID ex = service.create(tutorActor, space.getSpaceId(), request("TUTOR", null, 1, true, true, qs)).summary().exerciseId();
        AttemptView a = service.start(learnerActor, space.getSpaceId(), ex);
        service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), a.questions().get(0).questionId(), json("{'value':true}"));
        service.saveAnswer(learnerActor, space.getSpaceId(), ex, a.attemptId(), a.questions().get(1).questionId(), json("{'text':'わたしは アン です'}"));
        AttemptView done = service.submit(learnerActor, space.getSpaceId(), ex, a.attemptId());
        assertThat(done.status()).isEqualTo("SUBMITTED");
        assertThat(done.finalScore()).isNull();
        assertThat(done.autoScore()).isEqualByComparingTo("2");
        assertThat(service.list(learnerActor, space.getSpaceId()).get(0).pendingGrading()).isTrue();

        assertStatus(() -> service.grade(tutorActor, space.getSpaceId(), ex, a.attemptId(),
                new GradeAttemptRequest(List.of(new GradeItem(a.questions().get(1).questionId(), new BigDecimal("9"), null)))),
                HttpStatus.BAD_REQUEST);
        AttemptView graded = service.grade(tutorActor, space.getSpaceId(), ex, a.attemptId(), new GradeAttemptRequest(List.of(
                new GradeItem(a.questions().get(1).questionId(), new BigDecimal("6.5"), "Thiếu phần sở thích"),
                new GradeItem(a.questions().get(0).questionId(), new BigDecimal("1"), null))));
        assertThat(graded.status()).isEqualTo("GRADED");
        assertThat(graded.finalScore()).isEqualByComparingTo("7.5");
        assertThat(graded.questions().get(1).tutorComment()).isEqualTo("Thiếu phần sở thích");

        // Gửi lại đúng điểm máy (2) cho câu máy chấm → không tính là gia sư sửa.
        AttemptView same = service.grade(tutorActor, space.getSpaceId(), ex, a.attemptId(), new GradeAttemptRequest(List.of(
                new GradeItem(a.questions().get(1).questionId(), new BigDecimal("6.5"), "Thiếu phần sở thích"),
                new GradeItem(a.questions().get(0).questionId(), new BigDecimal("2"), null))));
        assertThat(same.finalScore()).isEqualByComparingTo("8.5");
        assertThat(answerRows(a.attemptId(), a.questions().get(0).questionId()).getTutorScore()).isNull();
        assertStatus(() -> service.grade(learnerActor, space.getSpaceId(), ex, a.attemptId(), new GradeAttemptRequest(List.of())),
                HttpStatus.FORBIDDEN);
    }

    private ExerciseAnswer answerRows(UUID attemptId, UUID questionId) {
        return answers.findByAttempt_AttemptId(attemptId).stream()
                .filter(x -> x.getQuestionId().equals(questionId)).findFirst().orElseThrow();
    }
}
