package com.kizuna.features.progress.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenGrant;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenStatus;
import com.kizuna.features.progress.entity.ExerciseAttempt;
import com.kizuna.features.progress.entity.ExerciseAudio;
import com.kizuna.features.progress.entity.ExerciseQuestion;
import com.kizuna.features.progress.entity.OnlineExercise;
import com.kizuna.features.progress.repository.ExerciseAttemptRepository;
import com.kizuna.features.progress.repository.ExerciseAudioRepository;
import com.kizuna.features.progress.repository.ExerciseListenCountRepository;
import com.kizuna.features.progress.repository.ExerciseQuestionRepository;
import com.kizuna.features.progress.service.ExerciseListenService;
import com.kizuna.features.storage.R2StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Đếm số lần nghe ở máy chủ (FE-07).
 * - Câu nghe có giới hạn (maxPlays 1–5): học viên đang làm bài bấm nghe → máy chủ cộng 1 lần rồi mới cấp link tạm;
 *   hết lượt thì báo lỗi. Đổi máy, đổi trình duyệt, xóa dữ liệu trình duyệt đều không nghe thêm được.
 * - Link hết hạn giữa lúc đang nghe: xin lại link không tính thêm lần (tối đa {@value #MAX_RESUMES} lần,
 *   trong {@value #RESUME_WINDOW_MINUTES} phút từ lúc bắt đầu lần nghe đó).
 * - Câu không giới hạn: cấp link, không đếm. Bài đã nộp: xem lại qua link thường, không giới hạn.
 */
@Slf4j
@Service
public class ExerciseListenServiceImpl implements ExerciseListenService {

    static final int MAX_RESUMES = 2;
    static final int RESUME_WINDOW_MINUTES = 30;

    private final ClassroomService classroomService;
    private final ExerciseAttemptRepository attempts;
    private final ExerciseQuestionRepository questions;
    private final ExerciseAudioRepository audios;
    private final ExerciseListenCountRepository counts;
    private final ObjectProvider<R2StorageService> storage;
    private final ObjectMapper mapper;
    private final long urlTtlSeconds;
    private final Clock clock;

    @Autowired
    public ExerciseListenServiceImpl(ClassroomService classroomService, ExerciseAttemptRepository attempts,
                                     ExerciseQuestionRepository questions, ExerciseAudioRepository audios,
                                     ExerciseListenCountRepository counts, ObjectProvider<R2StorageService> storage,
                                     ObjectMapper mapper,
                                     @Value("${app.r2.presigned-url-ttl-seconds:900}") long urlTtlSeconds) {
        this(classroomService, attempts, questions, audios, counts, storage, mapper, urlTtlSeconds,
                Clock.system(OnlineExerciseServiceImpl.VN));
    }

    public ExerciseListenServiceImpl(ClassroomService classroomService, ExerciseAttemptRepository attempts,
                                     ExerciseQuestionRepository questions, ExerciseAudioRepository audios,
                                     ExerciseListenCountRepository counts, ObjectProvider<R2StorageService> storage,
                                     ObjectMapper mapper, long urlTtlSeconds, Clock clock) {
        this.classroomService = classroomService;
        this.attempts = attempts;
        this.questions = questions;
        this.audios = audios;
        this.counts = counts;
        this.storage = storage;
        this.mapper = mapper;
        this.urlTtlSeconds = urlTtlSeconds;
        this.clock = clock;
    }

    /** File nghe của một câu: mã file + số lần nghe tối đa (null = không giới hạn). */
    record QuestionAudio(UUID questionId, UUID audioId, Integer maxPlays) {
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListenStatus> status(CurrentActor actor, UUID spaceId, UUID attemptId) {
        ExerciseAttempt a = ownAttempt(actor, spaceId, attemptId);
        Map<UUID, Integer> used = counts.playsOf(attemptId);
        List<ListenStatus> out = new ArrayList<>();
        for (ExerciseQuestion q : questions.findByExercise_ExerciseIdOrderByPositionAsc(a.getExercise().getExerciseId())) {
            QuestionAudio qa = audioOf(q);
            if (qa == null) continue;
            int plays = used.getOrDefault(q.getQuestionId(), 0);
            out.add(new ListenStatus(q.getQuestionId(), plays, qa.maxPlays(), remaining(qa.maxPlays(), plays)));
        }
        return out;
    }

    @Override
    @Transactional
    public ListenGrant listen(CurrentActor actor, UUID spaceId, UUID attemptId, UUID questionId, boolean resume) {
        ExerciseAttempt a = ownAttempt(actor, spaceId, attemptId);
        OnlineExercise e = a.getExercise();
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (a.getStatus() != ExerciseAttempt.Status.IN_PROGRESS) {
            throw ClassroomException.conflict("Bài đã nộp. Bạn xem lại file nghe ở trang kết quả.");
        }
        if (pastDeadline(a, e, now)) throw ClassroomException.conflict("Đã hết giờ làm bài nên không nghe tiếp được.");
        QuestionAudio qa = questions.findByExercise_ExerciseIdOrderByPositionAsc(e.getExerciseId()).stream()
                .filter(q -> q.getQuestionId().equals(questionId)).findFirst()
                .map(this::audioOf)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy file nghe của câu này."));
        ExerciseAudio audio = audios.findByAudioIdAndSpaceId(qa.audioId(), spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy file nghe của câu này."));
        // Tạo link trước (ký tại máy chủ) rồi mới cộng lần nghe: tạo link lỗi thì học viên không bị mất lượt
        String url = signedUrl(audio);
        int plays;
        if (qa.maxPlays() == null) {
            plays = counts.playsOf(attemptId).getOrDefault(questionId, 0);
        } else if (resume) {
            if (!counts.resume(attemptId, questionId, now.minusMinutes(RESUME_WINDOW_MINUTES), MAX_RESUMES)) {
                throw ClassroomException.conflict("Không tiếp tục được lần nghe này. Bấm nghe lại (sẽ tính thêm 1 lần nghe).");
            }
            plays = counts.playsOf(attemptId).getOrDefault(questionId, 0);
        } else {
            Integer after = counts.countPlay(attemptId, questionId, qa.maxPlays(), now);
            if (after == null) throw ClassroomException.conflict("Bạn đã nghe hết " + qa.maxPlays() + " lần cho câu này.");
            plays = after;
        }
        return new ListenGrant(url, urlTtlSeconds, plays, qa.maxPlays(), remaining(qa.maxPlays(), plays));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean requiresCounting(UUID learnerId, UUID exerciseId, UUID audioId) {
        if (exerciseId == null || audioId == null) return false;
        boolean limited = questions.findByExercise_ExerciseIdOrderByPositionAsc(exerciseId).stream()
                .map(this::audioOf)
                .anyMatch(qa -> qa != null && audioId.equals(qa.audioId()) && qa.maxPlays() != null);
        if (!limited) return false;
        List<ExerciseAttempt> own = attempts.findByExercise_ExerciseIdOrderByAttemptNumberAsc(exerciseId).stream()
                .filter(x -> x.getLearner() != null && Objects.equals(x.getLearner().getLearnerId(), learnerId)).toList();
        boolean inProgress = own.stream().anyMatch(x -> x.getStatus() == ExerciseAttempt.Status.IN_PROGRESS);
        boolean submitted = own.stream().anyMatch(x -> x.getStatus() != ExerciseAttempt.Status.IN_PROGRESS);
        return inProgress || !submitted;
    }

    // ================================================================== phụ trợ

    /** Lượt làm bài của chính học viên, thuộc bài của lớp này. */
    private ExerciseAttempt ownAttempt(CurrentActor actor, UUID spaceId, UUID attemptId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean learner = actor.isLearner()
                && Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId());
        if (!learner) throw ClassroomException.forbidden("Chỉ học viên của lớp mới nghe được trong lúc làm bài.");
        if (attemptId == null) throw ClassroomException.notFound("Không tìm thấy lượt làm bài.");
        return attempts.findById(attemptId)
                .filter(a -> a.getExercise() != null && a.getExercise().getSpace() != null
                        && spaceId.equals(a.getExercise().getSpace().getSpaceId()))
                .filter(a -> a.getLearner() != null
                        && Objects.equals(a.getLearner().getLearnerId(), actor.learner().getLearnerId()))
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy lượt làm bài."));
    }

    /** Hết giờ làm bài (hoặc đã đóng đề với bài không giới hạn thời gian), tính cả thời gian ân hạn như khi nộp. */
    static boolean pastDeadline(ExerciseAttempt a, OnlineExercise e, OffsetDateTime now) {
        if (a.getDeadlineAt() != null) return now.isAfter(a.getDeadlineAt().plusSeconds(OnlineExerciseServiceImpl.GRACE_SECONDS));
        return e.getClosesAt() != null && now.isAfter(e.getClosesAt().plusSeconds(OnlineExerciseServiceImpl.GRACE_SECONDS));
    }

    /** Đọc khóa "audio" trong dữ liệu câu hỏi; null nếu câu không có file nghe. */
    QuestionAudio audioOf(ExerciseQuestion q) {
        if (q.getData() == null) return null;
        try {
            JsonNode a = mapper.readTree(q.getData()).get("audio");
            if (a == null || !a.isObject() || !a.hasNonNull("id")) return null;
            Integer max = a.hasNonNull("maxPlays") && a.get("maxPlays").asInt() > 0 ? a.get("maxPlays").asInt() : null;
            return new QuestionAudio(q.getQuestionId(), UUID.fromString(a.get("id").asText()), max);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Integer remaining(Integer maxPlays, int plays) {
        return maxPlays == null ? null : Math.max(0, maxPlays - plays);
    }

    private String signedUrl(ExerciseAudio audio) {
        R2StorageService r2 = storage.getIfAvailable();
        if (r2 == null) throw new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE, ExerciseAudioServiceImpl.NOT_CONFIGURED_MESSAGE);
        try {
            return r2.presignedGetUrl(audio.getStorageKey());
        } catch (BadRequestException ex) {
            throw ClassroomException.notFound("Không tìm thấy file nghe của câu này.");
        } catch (SdkException ex) {
            log.warn("[FE-07] Không tạo được link nghe R2: {}", ex.getMessage());
            throw new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE, "Kho lưu file nghe đang không phản hồi. Vui lòng thử lại sau.");
        }
    }
}
