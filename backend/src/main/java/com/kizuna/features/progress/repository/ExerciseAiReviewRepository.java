package com.kizuna.features.progress.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FE-15: bảng exercise_ai_reviews (gợi ý chấm của AI). Các bảng của FE-07 chỉ được ĐỌC ở đây.
 */
@Repository
public class ExerciseAiReviewRepository {

    /**
     * Một câu tự gõ (tự luận, điền khuyết) trong lượt đã nộp, chưa có gợi ý, gia sư chưa chấm.
     * response = JSON câu trả lời; correct = kết quả máy chấm (null = máy chưa kết luận, chờ chấm).
     */
    public record Candidate(UUID attemptId, UUID questionId, String questionType, String data, String prompt,
                            BigDecimal points, String response, String exerciseTitle, String instructions,
                            Boolean correct) {
    }

    public record ReviewRow(UUID questionId, String status, BigDecimal suggestedScore, String feedback,
                            OffsetDateTime updatedAt) {
    }

    /** Lượt làm bài thuộc đúng bài và đúng lớp. */
    public record AttemptRef(UUID attemptId, String status) {
    }

    /** Người nhận và nội dung thông báo khi AI chấm xong một lượt. */
    public record NotifyTarget(UUID tutorUserId, String learnerName, String exerciseTitle, UUID spaceId, UUID exerciseId) {
    }

    private static final String CANDIDATES = """
            SELECT a.attempt_id, a.question_id, q.question_type, q.data, q.prompt, q.points, a.response,
                   e.title, e.instructions, a.correct
            FROM exercise_answers a
            JOIN exercise_attempts t ON t.attempt_id = a.attempt_id
            JOIN exercise_questions q ON q.question_id = a.question_id
            JOIN online_exercises e ON e.exercise_id = t.exercise_id
            WHERE t.status = 'SUBMITTED'
              AND t.submitted_at >= ?
              AND q.question_type IN ('WRITTEN', 'FILL_BLANK')
              AND a.tutor_score IS NULL
              AND NOT EXISTS (SELECT 1 FROM exercise_ai_reviews r
                              WHERE r.attempt_id = a.attempt_id AND r.question_id = a.question_id)
            """;

    private final JdbcTemplate jdbc;

    public ExerciseAiReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Câu chờ AI của mọi lượt nộp từ submittedAfter, cũ nhất trước. */
    public List<Candidate> findCandidates(OffsetDateTime submittedAfter, int limit) {
        return jdbc.query(CANDIDATES + " ORDER BY t.submitted_at, q.position LIMIT ?",
                (rs, i) -> candidate(rs), ts(submittedAfter), limit);
    }

    /** Câu chờ AI (chưa giữ chỗ) của một lượt. */
    public List<Candidate> findCandidates(UUID attemptId, OffsetDateTime submittedAfter) {
        return jdbc.query(CANDIDATES + " AND a.attempt_id = ? ORDER BY q.position",
                (rs, i) -> candidate(rs), ts(submittedAfter), attemptId);
    }

    /** Giữ chỗ câu: true nếu máy này chèn được dòng (máy khác chưa nhận). */
    public boolean claim(UUID attemptId, UUID questionId) {
        return jdbc.update("""
                INSERT INTO exercise_ai_reviews (attempt_id, question_id, status)
                VALUES (?, ?, 'PENDING')
                ON CONFLICT (attempt_id, question_id) DO NOTHING
                """, attemptId, questionId) == 1;
    }

    public void finish(UUID attemptId, UUID questionId, String status, BigDecimal score, String feedback,
                       String model, String error) {
        jdbc.update("""
                UPDATE exercise_ai_reviews
                SET status = ?, suggested_score = ?, feedback = ?, model = ?, error_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE attempt_id = ? AND question_id = ?
                """, status, score, feedback, truncate(model, 100), truncate(error, 300), attemptId, questionId);
    }

    public List<ReviewRow> findByAttempt(UUID attemptId) {
        return jdbc.query("""
                SELECT question_id, status, suggested_score, feedback, updated_at
                FROM exercise_ai_reviews WHERE attempt_id = ?
                """, (rs, i) -> new ReviewRow(rs.getObject("question_id", UUID.class), rs.getString("status"),
                rs.getBigDecimal("suggested_score"), rs.getString("feedback"), odt(rs.getTimestamp("updated_at"))),
                attemptId);
    }

    /** Xóa gợi ý "chưa chấm được" và câu kẹt "đang chấm" (cập nhật trước staleBefore) để chấm lại. */
    public int deleteRetryable(UUID attemptId, OffsetDateTime staleBefore) {
        return jdbc.update("""
                DELETE FROM exercise_ai_reviews
                WHERE attempt_id = ? AND (status = 'FAILED' OR (status = 'PENDING' AND updated_at < ?))
                """, attemptId, ts(staleBefore));
    }

    public Optional<AttemptRef> findAttempt(UUID spaceId, UUID exerciseId, UUID attemptId) {
        return jdbc.query("""
                SELECT t.attempt_id, t.status
                FROM exercise_attempts t JOIN online_exercises e ON e.exercise_id = t.exercise_id
                WHERE t.attempt_id = ? AND t.exercise_id = ? AND e.space_id = ?
                """, (rs, i) -> new AttemptRef(rs.getObject("attempt_id", UUID.class), rs.getString("status")),
                attemptId, exerciseId, spaceId).stream().findFirst();
    }

    public Optional<NotifyTarget> findNotifyTarget(UUID attemptId) {
        return jdbc.query("""
                SELECT tu.user_id AS tutor_user_id, lu.full_name AS learner_name, e.title, e.space_id, e.exercise_id
                FROM exercise_attempts t
                JOIN online_exercises e ON e.exercise_id = t.exercise_id
                JOIN learning_spaces s ON s.space_id = e.space_id
                JOIN tutors tt ON tt.tutor_id = s.tutor_id
                JOIN users tu ON tu.user_id = tt.user_id
                JOIN learners l ON l.learner_id = t.learner_id
                JOIN users lu ON lu.user_id = l.user_id
                WHERE t.attempt_id = ?
                """, (rs, i) -> new NotifyTarget(rs.getObject("tutor_user_id", UUID.class), rs.getString("learner_name"),
                rs.getString("title"), rs.getObject("space_id", UUID.class), rs.getObject("exercise_id", UUID.class)),
                attemptId).stream().findFirst();
    }

    private static Candidate candidate(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Candidate(rs.getObject("attempt_id", UUID.class), rs.getObject("question_id", UUID.class),
                rs.getString("question_type"), rs.getString("data"), rs.getString("prompt"), rs.getBigDecimal("points"),
                rs.getString("response"), rs.getString("title"), rs.getString("instructions"),
                (Boolean) rs.getObject("correct"));
    }

    private static Timestamp ts(OffsetDateTime value) {
        return Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime odt(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
