package com.kizuna.features.progress.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Số lần nghe của câu nghe trong từng lượt làm bài (bảng exercise_listen_counts, migration 035).
 * Cộng lần nghe bằng một câu lệnh upsert có điều kiện nên hai yêu cầu cùng lúc không vượt được giới hạn.
 */
@Repository
@RequiredArgsConstructor
public class ExerciseListenCountRepository {

    private final JdbcTemplate jdbc;

    /** Số lần đã nghe theo câu hỏi của một lượt làm bài. */
    public Map<UUID, Integer> playsOf(UUID attemptId) {
        Map<UUID, Integer> out = new HashMap<>();
        jdbc.query("SELECT question_id, plays FROM exercise_listen_counts WHERE attempt_id = ?",
                rs -> { out.put(rs.getObject("question_id", UUID.class), rs.getInt("plays")); }, attemptId);
        return out;
    }

    /**
     * Cộng 1 lần nghe nếu chưa đạt maxPlays; trả số lần đã nghe sau khi cộng, hoặc null nếu đã hết lượt.
     * Mỗi lần nghe mới đặt lại resumes (số lần xin lại link giữa chừng).
     */
    public Integer countPlay(UUID attemptId, UUID questionId, int maxPlays, OffsetDateTime now) {
        List<Integer> rows = jdbc.queryForList("""
                INSERT INTO exercise_listen_counts (attempt_id, question_id, plays, resumes, last_played_at)
                VALUES (?, ?, 1, 0, ?)
                ON CONFLICT (attempt_id, question_id) DO UPDATE
                    SET plays = exercise_listen_counts.plays + 1, resumes = 0, last_played_at = EXCLUDED.last_played_at
                    WHERE exercise_listen_counts.plays < ?
                RETURNING plays
                """, Integer.class, attemptId, questionId, now, maxPlays);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Xin lại link giữa lúc đang nghe (link cũ hết hạn) mà không tính thêm lần nghe: chỉ được khi đã nghe ít nhất 1 lần,
     * lần nghe gần nhất bắt đầu từ {@code since} trở đi và chưa xin lại quá {@code maxResumes} lần. Trả true nếu được.
     */
    public boolean resume(UUID attemptId, UUID questionId, OffsetDateTime since, int maxResumes) {
        return jdbc.update("""
                UPDATE exercise_listen_counts SET resumes = resumes + 1
                WHERE attempt_id = ? AND question_id = ? AND plays > 0 AND resumes < ? AND last_played_at >= ?
                """, attemptId, questionId, maxResumes, since) == 1;
    }
}
