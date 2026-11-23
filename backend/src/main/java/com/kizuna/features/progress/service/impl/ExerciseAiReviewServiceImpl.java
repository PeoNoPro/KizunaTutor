package com.kizuna.features.progress.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.progress.dto.ExerciseAiReviewResponse;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.Candidate;
import com.kizuna.features.progress.repository.ExerciseAiReviewRepository.ReviewRow;
import com.kizuna.features.progress.service.ExerciseAiReviewService;
import com.kizuna.features.progress.util.ExerciseAiPrompt;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * FE-15. Bộ xử lý nền chọn câu tự luận không đáp án của lượt đã nộp, giữ chỗ từng câu (một máy một câu),
 * gọi AI, kiểm tra kết quả và lưu gợi ý vào exercise_ai_reviews. Không ghi vào bảng nào của FE-07.
 */
@Slf4j
@Service
public class ExerciseAiReviewServiceImpl implements ExerciseAiReviewService {

    public static final String PENDING = "PENDING";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";
    public static final String EMPTY_ANSWER_FEEDBACK = "Học viên chưa trả lời câu này.";
    public static final String MATCHED_FEEDBACK = "Câu trả lời khớp đáp án gia sư đã nhập.";
    public static final String MSG_LIST = "Tải gợi ý chấm của AI thành công.";
    public static final String MSG_RETRY = "Đã gửi lại cho AI chấm. Kết quả sẽ có trong ít phút.";
    public static final String MSG_FORBIDDEN = "Chỉ gia sư của lớp mới xem được gợi ý chấm của AI.";
    public static final String MSG_NOT_FOUND = "Không tìm thấy lượt làm bài.";
    public static final String MSG_NOT_SUBMITTED = "Học viên chưa nộp lượt làm bài này.";
    public static final String MSG_UNAVAILABLE = "Chức năng AI chấm bài chưa sẵn sàng. Vui lòng thử lại sau.";
    public static final String NOTIFY_TITLE = "AI đã gợi ý điểm bài làm";

    /** Chỉ chấm lượt nộp trong khoảng này (tránh gọi AI hàng loạt cho dữ liệu cũ). */
    static final Duration WINDOW = Duration.ofDays(7);
    /** Câu "đang chấm" lâu hơn khoảng này coi như máy xử lý đã dừng. */
    static final Duration STALE = Duration.ofMinutes(10);
    static final int BATCH = 20;

    private final ExerciseAiReviewRepository repository;
    private final ExerciseAiClient client;
    private final ExerciseQuestionEngine engine;
    private final ObjectMapper mapper;
    private final ClassroomService classroomService;
    private final NotificationEvents notifications;
    private final Clock clock;

    @Autowired
    public ExerciseAiReviewServiceImpl(ExerciseAiReviewRepository repository, ExerciseAiClient client,
                                       ExerciseQuestionEngine engine, ObjectMapper mapper,
                                       ClassroomService classroomService, NotificationEvents notifications) {
        this(repository, client, engine, mapper, classroomService, notifications, Clock.systemUTC());
    }

    public ExerciseAiReviewServiceImpl(ExerciseAiReviewRepository repository, ExerciseAiClient client,
                                       ExerciseQuestionEngine engine, ObjectMapper mapper,
                                       ClassroomService classroomService, NotificationEvents notifications, Clock clock) {
        this.repository = repository;
        this.client = client;
        this.engine = engine;
        this.mapper = mapper;
        this.classroomService = classroomService;
        this.notifications = notifications;
        this.clock = clock;
    }

    // ================================================================== xử lý nền

    @Override
    public int processPending() {
        OffsetDateTime cutoff = now().minus(WINDOW);
        Set<UUID> touched = new LinkedHashSet<>();
        int processed = 0;
        for (Candidate c : repository.findCandidates(cutoff, BATCH)) {
            if (!needsAi(c) || !repository.claim(c.attemptId(), c.questionId())) {
                continue;
            }
            touched.add(c.attemptId());
            grade(c);
            processed++;
        }
        touched.forEach(attemptId -> notifyIfComplete(attemptId, cutoff));
        return processed;
    }

    /**
     * Câu đang chờ gia sư chấm theo đúng quy tắc của FE-07 (OnlineExerciseServiceImpl.awaitsReview, code của Linh):
     * câu luôn chấm tay (tự luận không đáp án, điền khuyết có ô không đáp án), hoặc máy chưa kết luận được
     * (correct null — điền khuyết học viên điền khác đáp án). Chỉ dùng hàm công khai của ExerciseQuestionEngine.
     */
    private boolean needsAi(Candidate c) {
        ExerciseQuestionType type = typeOf(c);
        if (type != ExerciseQuestionType.FILL_BLANK && type != ExerciseQuestionType.WRITTEN) return false;
        return engine.needsManualGrading(type, c.data())
                || (c.correct() == null && engine.mayNeedReview(type, c.data()));
    }

    private static ExerciseQuestionType typeOf(Candidate c) {
        try {
            return ExerciseQuestionType.valueOf(c.questionType());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private void grade(Candidate c) {
        ExerciseQuestionType type = typeOf(c);
        JsonNode data = engine.read(c.data());
        JsonNode response = engine.read(c.response());
        String answer = type == ExerciseQuestionType.FILL_BLANK ? blanksText(response) : answerText(c.response());
        if (answer.isBlank()) {
            repository.finish(c.attemptId(), c.questionId(), DONE, BigDecimal.ZERO.setScale(2), EMPTY_ANSWER_FEEDBACK,
                    null, null);
            return;
        }
        // Khớp đáp án gia sư đã nhập → gợi ý đủ điểm, không cần gọi AI
        if (!engine.needsManualGrading(type, c.data())
                && Boolean.TRUE.equals(engine.grade(type, c.data(), c.points(), response).correct())) {
            repository.finish(c.attemptId(), c.questionId(), DONE, c.points().setScale(2), MATCHED_FEEDBACK, null, null);
            return;
        }
        String prompt = c.prompt() == null ? "" : c.prompt().strip();
        if (type == ExerciseQuestionType.FILL_BLANK) {
            prompt = (prompt.isEmpty() ? "" : prompt + "\n") + "Câu có chỗ trống (___): " + data.path("text").asText("");
        }
        ExerciseAiPrompt.Question question = new ExerciseAiPrompt.Question(c.exerciseTitle(), c.instructions(),
                prompt, c.points(), data.path("sample").asText(""), answer, acceptedText(type, data));
        try {
            ExerciseAiClient.Reply reply = client.complete(ExerciseAiPrompt.build(question));
            ExerciseAiPrompt.Result result = ExerciseAiPrompt.parse(reply.text(), c.points(), question.truncated(), mapper);
            repository.finish(c.attemptId(), c.questionId(), DONE, result.score(), result.feedback(), reply.model(), null);
        } catch (ExerciseAiClient.AiUnavailableException | ExerciseAiPrompt.InvalidResultException e) {
            log.info("AI chưa chấm được câu {} của lượt {}: {}", c.questionId(), c.attemptId(), e.getMessage());
            repository.finish(c.attemptId(), c.questionId(), FAILED, null, null, null, e.getMessage());
        } catch (DataAccessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Lỗi khi AI chấm câu {} của lượt {}: {}", c.questionId(), c.attemptId(), e.getClass().getSimpleName());
            repository.finish(c.attemptId(), c.questionId(), FAILED, null, null, null,
                    "Lỗi xử lý: " + e.getClass().getSimpleName());
        }
    }

    /** Câu trả lời điền khuyết dạng "Chỗ 1: ...\nChỗ 2: ..." (rỗng nếu học viên không điền chỗ nào). */
    private static String blanksText(JsonNode response) {
        JsonNode blanks = response == null ? null : response.get("blanks");
        if (blanks == null || !blanks.isArray()) return "";
        StringBuilder out = new StringBuilder();
        boolean any = false;
        for (int i = 0; i < blanks.size(); i++) {
            String v = blanks.get(i).asText("").strip();
            any |= !v.isEmpty();
            out.append("Chỗ ").append(i + 1).append(": ").append(v.isEmpty() ? "(để trống)" : v).append('\n');
        }
        return any ? out.toString().strip() : "";
    }

    /** Đáp án gia sư chấp nhận, ghép thành chữ cho lời nhắc. */
    private static String acceptedText(ExerciseQuestionType type, JsonNode data) {
        StringBuilder out = new StringBuilder();
        if (type == ExerciseQuestionType.FILL_BLANK) {
            JsonNode answers = data.get("answers");
            if (answers != null && answers.isArray()) {
                for (int i = 0; i < answers.size(); i++) {
                    out.append(i == 0 ? "" : "; ").append("chỗ ").append(i + 1).append(": ").append(join(answers.get(i)));
                }
            }
        } else {
            out.append(join(data.get("accepted")));
        }
        return out.toString();
    }

    private static String join(JsonNode node) {
        if (node == null) return "";
        if (!node.isArray()) return node.asText("");
        StringBuilder out = new StringBuilder();
        node.forEach(n -> {
            String v = n.asText("").strip();
            if (!v.isEmpty()) out.append(out.length() == 0 ? "" : " / ").append(v);
        });
        return out.toString();
    }

    private String answerText(String response) {
        if (response == null || response.isBlank()) return "";
        JsonNode text = engine.read(response).get("text");
        return text != null && text.isTextual() ? text.asText().strip() : "";
    }

    /** Gửi một thông báo cho gia sư khi mọi câu tự gõ của lượt đã có kết quả. */
    private void notifyIfComplete(UUID attemptId, OffsetDateTime cutoff) {
        List<ReviewRow> rows = repository.findByAttempt(attemptId);
        if (rows.isEmpty() || rows.stream().anyMatch(r -> PENDING.equals(r.status()))) return;
        if (repository.findCandidates(attemptId, cutoff).stream().anyMatch(this::needsAi)) return;
        repository.findNotifyTarget(attemptId).ifPresent(t -> {
            long done = rows.stream().filter(r -> DONE.equals(r.status())).count();
            long failed = rows.size() - done;
            StringBuilder content = new StringBuilder("Bài \"").append(t.exerciseTitle()).append("\" của ")
                    .append(t.learnerName() == null ? "học viên" : t.learnerName()).append(": ");
            content.append(done > 0 ? "AI đã gợi ý điểm " + done + " câu tự viết. Vui lòng xem lại và lưu điểm."
                    : "AI chưa chấm được câu tự viết nào. Vui lòng chấm và lưu điểm.");
            if (failed > 0 && done > 0) {
                content.append(" Có ").append(failed).append(" câu AI chưa chấm được, bạn chấm tay hoặc chấm lại bằng AI.");
            }
            String path = "/pages/exercise-editor/index.html?space=" + t.spaceId() + "&exercise=" + t.exerciseId()
                    + "&tab=bai-lam";
            try {
                notifications.publish("exercise-ai:" + attemptId, "ASSIGNMENT", NOTIFY_TITLE, content.toString(), path,
                        List.of(t.tutorUserId()));
            } catch (RuntimeException e) {
                log.warn("Không gửi được thông báo AI chấm xong lượt {}: {}", attemptId, e.getMessage());
            }
        });
    }

    // ================================================================== gia sư xem / chấm lại

    @Override
    public List<ExerciseAiReviewResponse> list(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        if (!isTutorOf(actor, space)) {
            throw ClassroomException.forbidden(MSG_FORBIDDEN);
        }
        return guarded(() -> {
            requireAttempt(spaceId, exerciseId, attemptId);
            return responses(attemptId);
        });
    }

    @Override
    public List<ExerciseAiReviewResponse> retry(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId) {
        classroomService.loadGradableSpace(actor, spaceId);
        return guarded(() -> {
            ExerciseAiReviewRepository.AttemptRef attempt = requireAttempt(spaceId, exerciseId, attemptId);
            if ("IN_PROGRESS".equals(attempt.status())) {
                throw ClassroomException.conflict(MSG_NOT_SUBMITTED);
            }
            repository.deleteRetryable(attemptId, now().minus(STALE));
            return responses(attemptId);
        });
    }

    private ExerciseAiReviewRepository.AttemptRef requireAttempt(UUID spaceId, UUID exerciseId, UUID attemptId) {
        return repository.findAttempt(spaceId, exerciseId, attemptId)
                .orElseThrow(() -> ClassroomException.notFound(MSG_NOT_FOUND));
    }

    private List<ExerciseAiReviewResponse> responses(UUID attemptId) {
        OffsetDateTime staleBefore = now().minus(STALE);
        return repository.findByAttempt(attemptId).stream().map(r -> {
            String status = PENDING.equals(r.status()) && r.updatedAt() != null && r.updatedAt().isBefore(staleBefore)
                    ? FAILED : r.status();
            boolean done = DONE.equals(status);
            return new ExerciseAiReviewResponse(r.questionId(), status, label(status),
                    done ? r.suggestedScore() : null, done ? r.feedback() : null, r.updatedAt());
        }).toList();
    }

    /** Bảng gợi ý chưa được tạo (chưa chạy migration 036) → lỗi thân thiện thay vì lỗi hệ thống. */
    private <T> T guarded(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            log.warn("Không đọc được gợi ý chấm của AI: {}", e.getClass().getSimpleName());
            throw new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE, MSG_UNAVAILABLE);
        }
    }

    static String label(String status) {
        return switch (status) {
            case DONE -> "AI đã gợi ý";
            case FAILED -> "AI chưa chấm được";
            default -> "AI đang chấm";
        };
    }

    private static boolean isTutorOf(CurrentActor actor, LearningSpace space) {
        return actor != null && actor.tutor() != null && space.getTutor() != null
                && actor.tutor().getTutorId() != null
                && actor.tutor().getTutorId().equals(space.getTutor().getTutorId());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
