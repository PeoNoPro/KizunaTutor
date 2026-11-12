package com.kizuna.features.progress.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.kizuna.core.entity.User;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.*;
import com.kizuna.features.progress.entity.ExerciseAnswer;
import com.kizuna.features.progress.entity.ExerciseAttempt;
import com.kizuna.features.progress.entity.ExerciseQuestion;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseAnswerRepository;
import com.kizuna.features.progress.repository.ExerciseAttemptRepository;
import com.kizuna.features.progress.repository.ExerciseQuestionRepository;
import com.kizuna.features.progress.repository.OnlineExerciseRepository;
import com.kizuna.features.progress.service.OnlineExerciseService;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bài làm trực tiếp (FE-07).
 * - Gia sư của lớp (gói còn hiệu lực, lớp đang hoạt động) soạn / sửa / xóa đề; lưu nháp hoặc giao bài.
 * - Đã có học viên làm bài thì khóa câu hỏi, đáp án, thời gian làm bài và cách chấm; vẫn sửa được tiêu đề,
 *   hướng dẫn, giờ mở / đóng đề, số lượt làm (không nhỏ hơn số lượt đã dùng) và tùy chọn hiện đáp án.
 * - Học viên chỉ thấy bài đã giao và đã tới giờ mở. Một lượt được tính từ lúc bắt đầu; tắt trang thì làm tiếp lượt đó.
 * - Hết giờ làm bài (hoặc đóng đề với bài không giới hạn thời gian) thì tự nộp với các câu đã lưu;
 *   việc tự nộp được thực hiện ngay khi có bất kỳ ai mở bài / danh sách.
 * - Chấm: đúng hết mới có điểm; câu tự luận không có đáp án sẵn chờ gia sư chấm. Nhiều lượt → lấy điểm cao nhất.
 */
@Service
public class OnlineExerciseServiceImpl implements OnlineExerciseService {

    static final int TITLE_MAX = 150;
    static final int INSTRUCTIONS_MAX = 5000;
    static final int MAX_QUESTIONS = 200;
    static final int MAX_ATTEMPTS = 20;
    static final int MAX_TIME_LIMIT = 600;
    static final int RESPONSE_MAX = 20000;
    static final int COMMENT_MAX = 5000;
    /** Cho phép chậm vài giây khi lưu / nộp do đường truyền. */
    static final long GRACE_SECONDS = 30;
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassroomService classroomService;
    private final OnlineExerciseRepository exercises;
    private final ExerciseQuestionRepository questions;
    private final ExerciseAttemptRepository attempts;
    private final ExerciseAnswerRepository answers;
    private final LearningSpaceRepository spaces;
    private final ExerciseQuestionEngine engine;
    private final Clock clock;

    @Autowired
    public OnlineExerciseServiceImpl(ClassroomService classroomService, OnlineExerciseRepository exercises,
                                     ExerciseQuestionRepository questions, ExerciseAttemptRepository attempts,
                                     ExerciseAnswerRepository answers, LearningSpaceRepository spaces,
                                     ExerciseQuestionEngine engine) {
        this(classroomService, exercises, questions, attempts, answers, spaces, engine, Clock.system(VN));
    }

    public OnlineExerciseServiceImpl(ClassroomService classroomService, OnlineExerciseRepository exercises,
                                     ExerciseQuestionRepository questions, ExerciseAttemptRepository attempts,
                                     ExerciseAnswerRepository answers, LearningSpaceRepository spaces,
                                     ExerciseQuestionEngine engine, Clock clock) {
        this.classroomService = classroomService;
        this.exercises = exercises;
        this.questions = questions;
        this.attempts = attempts;
        this.answers = answers;
        this.spaces = spaces;
        this.engine = engine;
        this.clock = clock;
    }

    // ================================================================== xem

    @Override
    @Transactional
    public List<ExerciseSummary> list(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        OffsetDateTime now = now();
        boolean tutor = isTutorOf(actor, space);
        List<OnlineExercise> all = exercises.findBySpace_SpaceIdOrderByCreatedAtDesc(spaceId).stream()
                .filter(e -> tutor || visibleToLearner(e, now))
                .toList();
        if (all.isEmpty()) return List.of();
        List<UUID> ids = all.stream().map(OnlineExercise::getExerciseId).toList();
        Map<UUID, List<ExerciseQuestion>> qs = questions.findByExercise_ExerciseIdIn(ids).stream()
                .collect(Collectors.groupingBy(q -> q.getExercise().getExerciseId()));
        Map<UUID, List<ExerciseAttempt>> as = attempts.findByExercise_ExerciseIdInOrderByAttemptNumberAsc(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getExercise().getExerciseId()));
        boolean readOnly = classroomService.isReadOnly(space);
        return all.stream().map(e -> {
            List<ExerciseQuestion> eq = sorted(qs.getOrDefault(e.getExerciseId(), List.of()));
            List<ExerciseAttempt> ea = as.getOrDefault(e.getExerciseId(), List.of());
            ea.forEach(a -> finalizeIfDue(a, e, eq, now));
            return toSummary(e, eq, ea, tutor, readOnly, now);
        }).toList();
    }

    @Override
    @Transactional
    public ExerciseDetail detail(CurrentActor actor, UUID spaceId, UUID exerciseId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        OffsetDateTime now = now();
        boolean tutor = isTutorOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        if (!tutor) {
            requireLearnerOf(actor, space);
            if (!visibleToLearner(e, now)) throw notFound();
        }
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        List<ExerciseAttempt> ea = attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exerciseId);
        ea.forEach(a -> finalizeIfDue(a, e, eq, now));
        ExerciseSummary summary = toSummary(e, eq, ea, tutor, classroomService.isReadOnly(space), now);
        List<QuestionDetail> details = tutor ? eq.stream().map(q -> new QuestionDetail(q.getQuestionId(), q.getPosition(),
                q.getType(), q.getPrompt(), q.getPoints(), engine.read(q.getData()))).toList() : null;
        return new ExerciseDetail(summary, e.getInstructions(), details);
    }

    // ================================================================== gia sư soạn đề

    @Override
    @Transactional
    public ExerciseDetail create(CurrentActor actor, UUID spaceId, ExerciseRequest request) {
        LearningSpace space = loadTutorSpace(actor, spaceId);
        ExerciseRequest req = request == null ? emptyRequest() : request;
        OffsetDateTime now = now();
        boolean publish = Boolean.TRUE.equals(req.publish());
        OnlineExercise e = OnlineExercise.builder().space(space).build();
        applySettings(e, req, false, 0, publish, now);
        e = exercises.save(e);
        saveQuestions(e, req.questions(), publish);
        if (publish) {
            e.setStatus(OnlineExercise.Status.PUBLISHED);
            e.setPublishedAt(now);
            e = exercises.save(e);
        }
        return detail(actor, spaceId, e.getExerciseId());
    }

    @Override
    @Transactional
    public ExerciseDetail update(CurrentActor actor, UUID spaceId, UUID exerciseId, ExerciseRequest request) {
        loadTutorSpace(actor, spaceId);
        OnlineExercise e = find(exerciseId, spaceId);
        ExerciseRequest req = request == null ? emptyRequest() : request;
        OffsetDateTime now = now();
        List<ExerciseAttempt> ea = attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exerciseId);
        boolean locked = !ea.isEmpty();
        int used = ea.stream().mapToInt(ExerciseAttempt::getAttemptNumber).max().orElse(0);
        boolean publishNow = Boolean.TRUE.equals(req.publish()) && e.getStatus() == OnlineExercise.Status.DRAFT;
        boolean strict = publishNow || e.getStatus() == OnlineExercise.Status.PUBLISHED;
        applySettings(e, req, locked, used, strict, now);
        if (!locked) {
            questions.deleteByExercise_ExerciseId(exerciseId);
            questions.flush();
            saveQuestions(e, req.questions(), strict);
        }
        if (publishNow) {
            e.setStatus(OnlineExercise.Status.PUBLISHED);
            e.setPublishedAt(now);
        }
        exercises.save(e);
        return detail(actor, spaceId, exerciseId);
    }

    @Override
    @Transactional
    public void delete(CurrentActor actor, UUID spaceId, UUID exerciseId) {
        loadTutorSpace(actor, spaceId);
        OnlineExercise e = find(exerciseId, spaceId);
        if (attempts.existsByExercise_ExerciseId(exerciseId)) {
            throw ClassroomException.conflict("Học viên đã làm bài này nên không thể xóa.");
        }
        exercises.delete(e);
    }

    // ================================================================== học viên làm bài

    @Override
    @Transactional
    public AttemptView start(CurrentActor actor, UUID spaceId, UUID exerciseId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        requireLearnerOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        OffsetDateTime now = now();
        if (!visibleToLearner(e, now)) throw notFound();
        // Khóa lớp để bấm "Bắt đầu" hai lần cùng lúc không tạo hai lượt
        spaces.lockById(space.getSpaceId());
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        List<ExerciseAttempt> ea = attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exerciseId);
        ea.forEach(a -> finalizeIfDue(a, e, eq, now));
        Optional<ExerciseAttempt> inProgress = ea.stream()
                .filter(a -> a.getStatus() == ExerciseAttempt.Status.IN_PROGRESS).findFirst();
        if (inProgress.isPresent()) return toView(inProgress.get(), e, eq, false, now);

        if (classroomService.isReadOnly(space)) {
            throw ClassroomException.conflict("Lớp học đang ở chế độ chỉ xem nên không thể làm bài.");
        }
        if (e.getClosesAt() != null && !now.isBefore(e.getClosesAt())) {
            throw ClassroomException.conflict("Đề đã đóng nên không thể bắt đầu làm bài.");
        }
        int used = ea.stream().mapToInt(ExerciseAttempt::getAttemptNumber).max().orElse(0);
        if (used >= e.getMaxAttempts()) {
            throw ClassroomException.conflict("Bạn đã dùng hết " + e.getMaxAttempts() + " lượt làm bài này.");
        }
        if (eq.isEmpty()) throw ClassroomException.conflict("Bài tập chưa có câu hỏi.");
        ExerciseAttempt a = attempts.save(ExerciseAttempt.builder()
                .exercise(e)
                .learner(actor.learner())
                .attemptNumber(used + 1)
                .startedAt(now)
                .deadlineAt(e.getTimeLimitMinutes() == null ? null : now.plusMinutes(e.getTimeLimitMinutes()))
                .status(ExerciseAttempt.Status.IN_PROGRESS)
                .maxScore(maxScore(eq))
                .build());
        return toView(a, e, eq, false, now);
    }

    @Override
    @Transactional
    public List<AttemptView> attempts(CurrentActor actor, UUID spaceId, UUID exerciseId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean tutor = isTutorOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        OffsetDateTime now = now();
        if (!tutor) {
            requireLearnerOf(actor, space);
            if (!visibleToLearner(e, now)) throw notFound();
        }
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        List<ExerciseAttempt> ea = attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exerciseId);
        ea.forEach(a -> finalizeIfDue(a, e, eq, now));
        return ea.stream().map(a -> toView(a, e, eq, tutor, now)).toList();
    }

    @Override
    @Transactional
    public AttemptView attempt(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean tutor = isTutorOf(actor, space);
        if (!tutor) requireLearnerOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        ExerciseAttempt a = findAttempt(attemptId, exerciseId);
        OffsetDateTime now = now();
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        finalizeIfDue(a, e, eq, now);
        return toView(a, e, eq, tutor, now);
    }

    @Override
    @Transactional
    public void saveAnswer(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId, UUID questionId,
                           JsonNode response) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        requireLearnerOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        ExerciseAttempt a = findAttempt(attemptId, exerciseId);
        OffsetDateTime now = now();
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        finalizeIfDue(a, e, eq, now);
        if (a.getStatus() != ExerciseAttempt.Status.IN_PROGRESS) {
            throw ClassroomException.conflict("Bài đã nộp nên không thể sửa đáp án.");
        }
        if (eq.stream().noneMatch(q -> q.getQuestionId().equals(questionId))) {
            throw ClassroomException.notFound("Không tìm thấy câu hỏi trong bài này.");
        }
        String json = response == null || response.isNull() ? null : engine.write(response);
        if (json != null && json.length() > RESPONSE_MAX) {
            throw ClassroomException.field("response", "Câu trả lời quá dài.");
        }
        ExerciseAnswer answer = answers.findByAttempt_AttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> ExerciseAnswer.builder().attempt(a).questionId(questionId).build());
        answer.setResponse(json);
        answers.save(answer);
    }

    @Override
    @Transactional
    public AttemptView submit(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        requireLearnerOf(actor, space);
        OnlineExercise e = find(exerciseId, spaceId);
        ExerciseAttempt a = findAttempt(attemptId, exerciseId);
        OffsetDateTime now = now();
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        finalizeIfDue(a, e, eq, now);
        if (a.getStatus() == ExerciseAttempt.Status.IN_PROGRESS) {
            finalizeAttempt(a, e, eq, "MANUAL", now);
        }
        return toView(a, e, eq, false, now);
    }

    // ================================================================== gia sư chấm

    @Override
    @Transactional
    public AttemptView grade(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId, GradeAttemptRequest request) {
        // Khóa học đã hoàn thành vẫn chấm được câu tự luận còn chờ
        classroomService.loadGradableSpace(actor, spaceId);
        OnlineExercise e = find(exerciseId, spaceId);
        ExerciseAttempt a = findAttempt(attemptId, exerciseId);
        OffsetDateTime now = now();
        List<ExerciseQuestion> eq = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId);
        finalizeIfDue(a, e, eq, now);
        if (a.getStatus() == ExerciseAttempt.Status.IN_PROGRESS) {
            throw ClassroomException.conflict("Học viên chưa nộp lượt làm bài này.");
        }
        Map<UUID, ExerciseQuestion> byId = eq.stream().collect(Collectors.toMap(ExerciseQuestion::getQuestionId, q -> q));
        Map<UUID, ExerciseAnswer> existing = answers.findByAttempt_AttemptId(attemptId).stream()
                .collect(Collectors.toMap(ExerciseAnswer::getQuestionId, x -> x, (x, y) -> x));
        for (GradeItem item : request == null || request.items() == null ? List.<GradeItem>of() : request.items()) {
            ExerciseQuestion q = byId.get(item.questionId());
            if (q == null) throw ClassroomException.notFound("Không tìm thấy câu hỏi trong bài này.");
            String label = "Câu " + q.getPosition();
            if (item.score() != null && (item.score().signum() < 0 || item.score().compareTo(q.getPoints()) > 0)) {
                throw ClassroomException.field("grade", label + ": điểm phải từ 0 đến "
                        + q.getPoints().stripTrailingZeros().toPlainString() + ".");
            }
            String comment = item.comment() == null || item.comment().isBlank() ? null : item.comment().strip();
            if (comment != null && comment.length() > COMMENT_MAX) {
                throw ClassroomException.field("grade", label + ": nhận xét tối đa " + COMMENT_MAX + " ký tự.");
            }
            ExerciseAnswer ans = existing.computeIfAbsent(q.getQuestionId(),
                    id -> ExerciseAnswer.builder().attempt(a).questionId(id).correct(false).autoScore(BigDecimal.ZERO).build());
            // Câu máy đã chấm: điểm gia sư trùng điểm máy coi như không sửa (học viên không thấy "gia sư sửa điểm").
            BigDecimal score = item.score() == null ? null : item.score().setScale(2, RoundingMode.HALF_UP);
            if (score != null && !engine.needsManualGrading(q.getType(), q.getData())
                    && ans.getAutoScore() != null && score.compareTo(ans.getAutoScore()) == 0) {
                score = null;
            }
            ans.setTutorScore(score);
            ans.setTutorComment(comment);
            answers.save(ans);
        }
        recompute(a, eq, new ArrayList<>(existing.values()));
        attempts.save(a);
        return toView(a, e, eq, true, now);
    }

    // ================================================================== chấm / tự nộp

    /** Hết giờ (hoặc đóng đề với bài không giới hạn thời gian) mà chưa nộp → tự nộp với các câu đã lưu. */
    void finalizeIfDue(ExerciseAttempt a, OnlineExercise e, List<ExerciseQuestion> eq, OffsetDateTime now) {
        if (a.getStatus() != ExerciseAttempt.Status.IN_PROGRESS) return;
        if (a.getDeadlineAt() != null) {
            if (now.isAfter(a.getDeadlineAt().plusSeconds(GRACE_SECONDS))) finalizeAttempt(a, e, eq, "TIMEOUT", a.getDeadlineAt());
        } else if (e.getClosesAt() != null && now.isAfter(e.getClosesAt().plusSeconds(GRACE_SECONDS))) {
            finalizeAttempt(a, e, eq, "CLOSED", e.getClosesAt());
        }
    }

    /** Nộp lượt: chấm từng câu, cộng điểm; còn câu tự luận chưa có đáp án → chờ gia sư chấm. */
    void finalizeAttempt(ExerciseAttempt a, OnlineExercise e, List<ExerciseQuestion> eq, String reason, OffsetDateTime at) {
        Map<UUID, ExerciseAnswer> byQuestion = answers.findByAttempt_AttemptId(a.getAttemptId()).stream()
                .collect(Collectors.toMap(ExerciseAnswer::getQuestionId, x -> x, (x, y) -> x));
        List<ExerciseAnswer> all = new ArrayList<>();
        for (ExerciseQuestion q : eq) {
            ExerciseAnswer ans = byQuestion.getOrDefault(q.getQuestionId(),
                    ExerciseAnswer.builder().attempt(a).questionId(q.getQuestionId()).build());
            ExerciseQuestionEngine.Grade g = engine.grade(q.getType(), q.getData(), q.getPoints(), engine.read(ans.getResponse()));
            ans.setCorrect(g.correct());
            ans.setAutoScore(g.score());
            all.add(answers.save(ans));
        }
        a.setSubmittedAt(at);
        a.setSubmitReason(reason);
        a.setLate(e.getClosesAt() != null && at.isAfter(e.getClosesAt()));
        recompute(a, eq, all);
        attempts.save(a);
    }

    /** Điểm lượt = tổng (điểm gia sư nếu có, ngược lại điểm máy). Còn câu tự luận chưa chấm → SUBMITTED, chưa có điểm cuối. */
    void recompute(ExerciseAttempt a, List<ExerciseQuestion> eq, List<ExerciseAnswer> all) {
        Map<UUID, ExerciseAnswer> byQuestion = all.stream()
                .collect(Collectors.toMap(ExerciseAnswer::getQuestionId, x -> x, (x, y) -> x));
        BigDecimal auto = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        boolean pending = false;
        for (ExerciseQuestion q : eq) {
            ExerciseAnswer ans = byQuestion.get(q.getQuestionId());
            BigDecimal autoScore = ans == null || ans.getAutoScore() == null ? BigDecimal.ZERO : ans.getAutoScore();
            auto = auto.add(autoScore);
            if (ans != null && ans.getTutorScore() != null) {
                total = total.add(ans.getTutorScore());
            } else if (engine.needsManualGrading(q.getType(), q.getData())) {
                pending = true;
            } else {
                total = total.add(autoScore);
            }
        }
        a.setAutoScore(auto.setScale(2, RoundingMode.HALF_UP));
        a.setStatus(pending ? ExerciseAttempt.Status.SUBMITTED : ExerciseAttempt.Status.GRADED);
        a.setFinalScore(pending ? null : total.setScale(2, RoundingMode.HALF_UP));
    }

    // ================================================================== dựng dữ liệu trả về

    private ExerciseSummary toSummary(OnlineExercise e, List<ExerciseQuestion> eq, List<ExerciseAttempt> ea,
                                      boolean tutor, boolean readOnly, OffsetDateTime now) {
        String state = stateOf(e, now);
        Optional<ExerciseAttempt> inProgress = ea.stream()
                .filter(a -> a.getStatus() == ExerciseAttempt.Status.IN_PROGRESS).findFirst();
        int used = ea.stream().mapToInt(ExerciseAttempt::getAttemptNumber).max().orElse(0);
        BigDecimal best = ea.stream().filter(a -> a.getStatus() == ExerciseAttempt.Status.GRADED && a.getFinalScore() != null)
                .map(ExerciseAttempt::getFinalScore).max(Comparator.naturalOrder()).orElse(null);
        boolean pending = ea.stream().anyMatch(a -> a.getStatus() == ExerciseAttempt.Status.SUBMITTED);
        boolean canStart = !tutor && !readOnly && "OPEN".equals(state)
                && (inProgress.isPresent() || used < e.getMaxAttempts()) && !eq.isEmpty();
        if (!tutor && inProgress.isPresent()) canStart = true;
        return new ExerciseSummary(e.getExerciseId(), e.getTitle(), e.getStatus().name(), state,
                e.getOpensAt(), e.getClosesAt(), e.getTimeLimitMinutes(), e.getMaxAttempts(), e.getGradingMode().name(),
                e.isShowAnswers(), eq.size(), maxScore(eq), used, inProgress.map(ExerciseAttempt::getAttemptId).orElse(null),
                best, pending, canStart, !ea.isEmpty(), tutor && !readOnly, e.getCreatedAt());
    }

    private AttemptView toView(ExerciseAttempt a, OnlineExercise e, List<ExerciseQuestion> eq, boolean forTutor,
                               OffsetDateTime now) {
        boolean inProgress = a.getStatus() == ExerciseAttempt.Status.IN_PROGRESS;
        boolean answersVisible = !inProgress && (forTutor || e.isShowAnswers());
        Map<UUID, ExerciseAnswer> byQuestion = answers.findByAttempt_AttemptId(a.getAttemptId()).stream()
                .collect(Collectors.toMap(ExerciseAnswer::getQuestionId, x -> x, (x, y) -> x));
        List<AttemptQuestion> items = eq.stream().map(q -> {
            ExerciseAnswer ans = byQuestion.get(q.getQuestionId());
            boolean manual = engine.needsManualGrading(q.getType(), q.getData());
            JsonNode response = ans == null || ans.getResponse() == null ? null : engine.read(ans.getResponse());
            Boolean correct = null;
            BigDecimal score = null;
            BigDecimal autoScore = null;
            String comment = null;
            if (!inProgress) {
                correct = ans == null ? (manual ? null : Boolean.FALSE) : ans.getCorrect();
                autoScore = ans == null ? BigDecimal.ZERO : ans.getAutoScore();
                if (ans != null && ans.getTutorScore() != null) score = ans.getTutorScore();
                else if (!manual) score = autoScore == null ? BigDecimal.ZERO : autoScore;
                comment = ans == null ? null : ans.getTutorComment();
            }
            return new AttemptQuestion(q.getQuestionId(), q.getPosition(), q.getType(), q.getPrompt(), q.getPoints(),
                    engine.learnerView(q.getType(), q.getData(), seed(a, q)), response, correct, score, autoScore,
                    manual, comment, answersVisible ? engine.correctAnswer(q.getType(), q.getData()) : null);
        }).toList();
        Long remaining = inProgress && a.getDeadlineAt() != null
                ? Math.max(0, Duration.between(now, a.getDeadlineAt()).getSeconds()) : null;
        User learnerUser = a.getLearner() == null ? null : a.getLearner().getUser();
        return new AttemptView(a.getAttemptId(), e.getExerciseId(), e.getTitle(), e.getInstructions(),
                a.getAttemptNumber(), e.getMaxAttempts(), a.getStatus().name(),
                a.getStartedAt(), a.getDeadlineAt(), e.getClosesAt(), a.getSubmittedAt(), now, remaining,
                a.getSubmitReason(), a.isLate(), inProgress ? null : a.getAutoScore(), a.getFinalScore(), a.getMaxScore(),
                answersVisible, learnerUser == null ? null : learnerUser.getFullName(), items);
    }

    // ================================================================== kiểm tra dữ liệu bài

    private void applySettings(OnlineExercise e, ExerciseRequest req, boolean locked, int usedAttempts, boolean strict,
                               OffsetDateTime now) {
        String title = req.title() == null ? "" : req.title().strip();
        if (title.isEmpty()) throw ClassroomException.field("title", "Vui lòng nhập tiêu đề bài tập.");
        if (title.length() > TITLE_MAX) throw ClassroomException.field("title", "Tiêu đề bài tập tối đa " + TITLE_MAX + " ký tự.");
        String instructions = req.instructions() == null || req.instructions().isBlank() ? null : req.instructions().strip();
        if (instructions != null && instructions.length() > INSTRUCTIONS_MAX) {
            throw ClassroomException.field("instructions", "Hướng dẫn làm bài tối đa " + INSTRUCTIONS_MAX + " ký tự.");
        }
        if (req.opensAt() != null && req.closesAt() != null && !req.opensAt().isBefore(req.closesAt())) {
            throw ClassroomException.field("closesAt", "Thời gian đóng đề phải sau thời gian mở đề.");
        }
        boolean closesChanged = !sameInstant(req.closesAt(), e.getClosesAt());
        if (strict && closesChanged && req.closesAt() != null && !req.closesAt().isAfter(now)) {
            throw ClassroomException.field("closesAt", "Thời gian đóng đề phải ở tương lai.");
        }
        int maxAttempts = req.maxAttempts() == null ? 1 : req.maxAttempts();
        if (maxAttempts < 1 || maxAttempts > MAX_ATTEMPTS) {
            throw ClassroomException.field("maxAttempts", "Số lượt làm bài phải từ 1 đến " + MAX_ATTEMPTS + ".");
        }
        if (maxAttempts < usedAttempts) {
            throw ClassroomException.field("maxAttempts", "Học viên đã làm " + usedAttempts
                    + " lượt nên số lượt làm bài phải từ " + usedAttempts + " trở lên.");
        }
        e.setTitle(title);
        e.setInstructions(instructions);
        e.setOpensAt(req.opensAt());
        e.setClosesAt(req.closesAt());
        e.setMaxAttempts(maxAttempts);
        e.setShowAnswers(req.showAnswers() == null || req.showAnswers());
        if (locked) return; // đã có người làm: giữ nguyên thời gian làm bài và cách chấm
        Integer limit = req.timeLimitMinutes();
        if (limit != null && (limit < 1 || limit > MAX_TIME_LIMIT)) {
            throw ClassroomException.field("timeLimitMinutes", "Thời gian làm bài phải từ 1 đến " + MAX_TIME_LIMIT + " phút, hoặc để trống nếu không giới hạn.");
        }
        e.setTimeLimitMinutes(limit);
        e.setGradingMode(parseMode(req.gradingMode()));
    }

    private void saveQuestions(OnlineExercise e, List<QuestionInput> inputs, boolean strict) {
        List<QuestionInput> list = inputs == null ? List.of() : inputs;
        if (list.size() > MAX_QUESTIONS) {
            throw ClassroomException.field(ExerciseQuestionEngine.FIELD, "Mỗi bài có tối đa " + MAX_QUESTIONS + " câu hỏi.");
        }
        if (strict && list.isEmpty()) {
            throw ClassroomException.field(ExerciseQuestionEngine.FIELD, "Bài tập cần có ít nhất một câu hỏi.");
        }
        boolean auto = e.getGradingMode() == OnlineExercise.GradingMode.AUTO;
        List<ExerciseQuestion> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            QuestionInput in = list.get(i);
            String label = "Câu " + (i + 1);
            BigDecimal points = in.points() == null && !strict ? BigDecimal.ONE : in.points();
            String data = engine.validate(in.type(), in.prompt(), points, in.data(), strict, auto, label);
            rows.add(ExerciseQuestion.builder()
                    .exercise(e)
                    .position(i + 1)
                    .type(in.type())
                    .prompt(in.prompt() == null || in.prompt().isBlank() ? null : in.prompt().strip())
                    .data(data)
                    .points(points.setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        questions.saveAll(rows);
    }

    // ================================================================== hỗ trợ

    private LearningSpace loadTutorSpace(CurrentActor actor, UUID spaceId) {
        if (!actor.isTutor()) throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được soạn và chấm bài làm trực tiếp.");
        return classroomService.loadManageableSpace(actor, spaceId);
    }

    private static void requireLearnerOf(CurrentActor actor, LearningSpace space) {
        if (!actor.isLearner() || !Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId())) {
            throw ClassroomException.forbidden("Chỉ học viên của lớp mới được làm bài.");
        }
    }

    private static boolean isTutorOf(CurrentActor actor, LearningSpace space) {
        return actor.isTutor() && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
    }

    private OnlineExercise find(UUID exerciseId, UUID spaceId) {
        return exercises.findByExerciseIdAndSpace_SpaceId(exerciseId, spaceId).orElseThrow(OnlineExerciseServiceImpl::notFound);
    }

    private ExerciseAttempt findAttempt(UUID attemptId, UUID exerciseId) {
        return attempts.findByAttemptIdAndExercise_ExerciseId(attemptId, exerciseId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy lượt làm bài."));
    }

    private static ClassroomException notFound() {
        return ClassroomException.notFound("Không tìm thấy bài tập trong lớp học này.");
    }

    static boolean visibleToLearner(OnlineExercise e, OffsetDateTime now) {
        return e.getStatus() == OnlineExercise.Status.PUBLISHED && (e.getOpensAt() == null || !now.isBefore(e.getOpensAt()));
    }

    static String stateOf(OnlineExercise e, OffsetDateTime now) {
        if (e.getStatus() == OnlineExercise.Status.DRAFT) return "DRAFT";
        if (e.getOpensAt() != null && now.isBefore(e.getOpensAt())) return "NOT_OPEN";
        if (e.getClosesAt() != null && !now.isBefore(e.getClosesAt())) return "CLOSED";
        return "OPEN";
    }

    private static OnlineExercise.GradingMode parseMode(String raw) {
        if (raw == null || raw.isBlank() || "AUTO".equalsIgnoreCase(raw)) return OnlineExercise.GradingMode.AUTO;
        if ("TUTOR".equalsIgnoreCase(raw)) return OnlineExercise.GradingMode.TUTOR;
        throw ClassroomException.field("gradingMode", "Cách chấm không hợp lệ.");
    }

    private static BigDecimal maxScore(List<ExerciseQuestion> eq) {
        return eq.stream().map(ExerciseQuestion::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static List<ExerciseQuestion> sorted(List<ExerciseQuestion> eq) {
        return eq.stream().sorted(Comparator.comparing(ExerciseQuestion::getPosition)).toList();
    }

    private static long seed(ExerciseAttempt a, ExerciseQuestion q) {
        return a.getAttemptId().getMostSignificantBits() ^ q.getQuestionId().getLeastSignificantBits();
    }

    private static boolean sameInstant(OffsetDateTime a, OffsetDateTime b) {
        if (a == null || b == null) return a == b;
        return a.isEqual(b);
    }

    private static ExerciseRequest emptyRequest() {
        return new ExerciseRequest(null, null, null, null, null, null, null, null, false, List.of());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
