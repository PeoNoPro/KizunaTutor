package com.kizuna.features.progress.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.kizuna.features.progress.entity.ExerciseQuestionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Dữ liệu vào / ra của bài làm trực tiếp (FE-07). */
public final class ExerciseDtos {

    private ExerciseDtos() {
    }

    /** Một câu hỏi gia sư soạn. data: JSON theo dạng câu (xem ExerciseQuestionType). */
    public record QuestionInput(ExerciseQuestionType type, String prompt, BigDecimal points, JsonNode data) {
    }

    /**
     * Tạo / sửa bài. publish = true: giao bài (kiểm tra đủ đáp án); false: lưu nháp (bài đã giao thì giữ nguyên trạng thái đã giao).
     * gradingMode: "AUTO" (hệ thống chấm tự động) hoặc "TUTOR" (gia sư chấm).
     */
    public record ExerciseRequest(String title, String instructions, OffsetDateTime opensAt, OffsetDateTime closesAt,
                                  Integer timeLimitMinutes, Integer maxAttempts, String gradingMode, Boolean showAnswers,
                                  Boolean publish, List<QuestionInput> questions) {
    }

    /**
     * Thông tin một bài trong danh sách.
     * state: DRAFT (nháp), NOT_OPEN (chưa mở), OPEN (đang mở), CLOSED (đã đóng).
     * locked: đã có học viên làm bài → khóa câu hỏi, đáp án, thời gian làm bài, cách chấm.
     */
    public record ExerciseSummary(UUID exerciseId, String title, String status, String state,
                                  OffsetDateTime opensAt, OffsetDateTime closesAt, Integer timeLimitMinutes,
                                  int maxAttempts, String gradingMode, boolean showAnswers,
                                  int questionCount, BigDecimal maxScore,
                                  int attemptsUsed, UUID inProgressAttemptId, BigDecimal bestScore,
                                  boolean pendingGrading, boolean canStart, boolean locked, boolean canManage,
                                  OffsetDateTime createdAt) {
    }

    /** Câu hỏi đầy đủ (kèm đáp án) cho gia sư soạn / sửa. */
    public record QuestionDetail(UUID questionId, int position, ExerciseQuestionType type, String prompt,
                                 BigDecimal points, JsonNode data) {
    }

    /** Chi tiết bài: gia sư nhận kèm câu hỏi đầy đủ; học viên nhận questions = null. */
    public record ExerciseDetail(ExerciseSummary summary, String instructions, List<QuestionDetail> questions) {
    }

    /**
     * Một câu trong một lượt làm bài.
     * view: đề cho học viên (không có đáp án). response: câu trả lời đã lưu.
     * Sau khi nộp: correct / score (null khi chờ gia sư chấm); correctAnswer chỉ có khi được xem đáp án.
     */
    public record AttemptQuestion(UUID questionId, int position, ExerciseQuestionType type, String prompt,
                                  BigDecimal points, JsonNode view, JsonNode response,
                                  Boolean correct, BigDecimal score, BigDecimal autoScore, boolean needsManualGrading,
                                  String tutorComment, JsonNode correctAnswer) {
    }

    /**
     * Một lượt làm bài. remainingSeconds: số giây còn lại (null = không giới hạn); serverNow để trình duyệt canh giờ.
     * status: IN_PROGRESS, SUBMITTED (chờ gia sư chấm), GRADED.
     */
    public record AttemptView(UUID attemptId, UUID exerciseId, String exerciseTitle, String instructions,
                              int attemptNumber, int maxAttempts, String status,
                              OffsetDateTime startedAt, OffsetDateTime deadlineAt, OffsetDateTime closesAt,
                              OffsetDateTime submittedAt, OffsetDateTime serverNow, Long remainingSeconds,
                              String submitReason, boolean late,
                              BigDecimal autoScore, BigDecimal finalScore, BigDecimal maxScore,
                              boolean answersVisible, String learnerName, List<AttemptQuestion> questions) {
    }

    /** Gia sư chấm / sửa điểm một lượt: mỗi phần tử là một câu (score null = bỏ điểm sửa, dùng lại điểm máy chấm). */
    public record GradeAttemptRequest(List<GradeItem> items) {
    }

    public record GradeItem(UUID questionId, BigDecimal score, String comment) {
    }
}
