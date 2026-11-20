-- 036 (FE-15): AI gợi ý chấm câu tự luận của bài làm trực tiếp (FE-07).
-- Trước đây tên 032_exercise_ai_reviews.sql (trùng số với 032_practice_exam_mode_only của nhóm); DB đã chạy bản cũ thì không cần chạy lại.
-- Chạy SAU migration 017. Chạy lại nhiều lần vẫn an toàn. Không sửa bảng nào của FE-07.
--
-- Mỗi dòng là gợi ý của AI cho một câu tự luận (không có đáp án) trong một lượt làm bài đã nộp.
-- Gợi ý KHÔNG phải điểm chính thức: điểm chính thức vẫn do gia sư lưu vào exercise_answers.tutor_score.
-- UNIQUE (attempt_id, question_id) dùng để giữ chỗ: nhiều máy chạy backend cùng DB thì chỉ một máy gọi AI cho mỗi câu.

CREATE TABLE IF NOT EXISTS exercise_ai_reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES exercise_attempts(attempt_id) ON DELETE CASCADE ON UPDATE CASCADE,
    question_id UUID NOT NULL REFERENCES exercise_questions(question_id) ON DELETE CASCADE ON UPDATE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',      -- PENDING = AI đang chấm, DONE = đã có gợi ý, FAILED = AI chưa chấm được
    suggested_score NUMERIC(6, 2),                       -- điểm gợi ý (chỉ khi DONE)
    feedback TEXT,                                       -- nhận xét gợi ý (tiếng Việt, tối đa 1.000 ký tự)
    model VARCHAR(100),                                  -- model AI đã dùng (NULL khi không gọi AI, ví dụ câu bỏ trống)
    error_message VARCHAR(300),                          -- lỗi nội bộ ngắn, không hiện cho người dùng
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exercise_ai_review UNIQUE (attempt_id, question_id),
    CONSTRAINT chk_exercise_ai_review_status CHECK (status IN ('PENDING', 'DONE', 'FAILED')),
    CONSTRAINT chk_exercise_ai_review_score CHECK (suggested_score IS NULL OR suggested_score >= 0)
);

CREATE INDEX IF NOT EXISTS idx_exercise_ai_reviews_question ON exercise_ai_reviews(question_id);
