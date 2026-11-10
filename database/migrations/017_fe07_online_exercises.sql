-- FE-07: Bài làm trực tiếp trên web (online exercise) + thời gian mở bài cho bài nộp tệp.
-- Chạy SAU migration 016. Chạy lại nhiều lần vẫn an toàn.
--
-- 1) assignments.opens_at: thời gian mở bài nộp tệp (NULL = mở ngay). Trước giờ mở học viên không thấy bài.
-- 2) online_exercises: bài làm trực tiếp (đề do gia sư soạn, học viên làm trên web, điểm riêng với bài nộp tệp).
-- 3) exercise_questions: câu hỏi của bài (dạng câu, đề, dữ liệu đáp án dạng JSON, điểm, thứ tự).
-- 4) exercise_attempts: mỗi lượt làm bài của học viên (bắt đầu, hạn chót của lượt, nộp, điểm).
-- 5) exercise_answers: câu trả lời từng câu trong một lượt (đúng/sai, điểm máy chấm, điểm gia sư sửa, nhận xét).

-- 1) Thời gian mở bài nộp tệp
ALTER TABLE assignments ADD COLUMN IF NOT EXISTS opens_at TIMESTAMPTZ;

-- 2) Bài làm trực tiếp
CREATE TABLE IF NOT EXISTS online_exercises (
    exercise_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES learning_spaces(space_id) ON DELETE CASCADE ON UPDATE CASCADE,
    title VARCHAR(150) NOT NULL,
    instructions TEXT,
    opens_at TIMESTAMPTZ,                       -- NULL = mở ngay khi giao
    closes_at TIMESTAMPTZ,                      -- NULL = không đóng đề
    time_limit_minutes INTEGER,                 -- NULL = không giới hạn thời gian làm bài
    max_attempts INTEGER NOT NULL DEFAULT 1,
    grading_mode VARCHAR(10) NOT NULL DEFAULT 'AUTO',     -- AUTO = hệ thống chấm tự động, TUTOR = gia sư chấm
    show_answers BOOLEAN NOT NULL DEFAULT TRUE,           -- cho học viên xem đáp án đúng sau khi nộp
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',          -- DRAFT = nháp (học viên không thấy), PUBLISHED = đã giao
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_online_exercise_attempts CHECK (max_attempts BETWEEN 1 AND 20),
    CONSTRAINT chk_online_exercise_time_limit CHECK (time_limit_minutes IS NULL OR time_limit_minutes BETWEEN 1 AND 600),
    CONSTRAINT chk_online_exercise_mode CHECK (grading_mode IN ('AUTO', 'TUTOR')),
    CONSTRAINT chk_online_exercise_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT chk_online_exercise_window CHECK (opens_at IS NULL OR closes_at IS NULL OR opens_at < closes_at)
);
CREATE INDEX IF NOT EXISTS idx_online_exercises_space ON online_exercises(space_id);

-- 3) Câu hỏi
CREATE TABLE IF NOT EXISTS exercise_questions (
    question_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES online_exercises(exercise_id) ON DELETE CASCADE ON UPDATE CASCADE,
    position INTEGER NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    prompt TEXT,
    data TEXT NOT NULL,                          -- JSON: lựa chọn, đáp án đúng, chỗ trống, cặp nối...
    points NUMERIC(6, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exercise_question_points CHECK (points > 0),
    CONSTRAINT chk_exercise_question_type CHECK (question_type IN
        ('SINGLE_CHOICE', 'MULTI_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'WORD_BANK', 'MATCHING', 'ORDERING', 'WRITTEN'))
);
CREATE INDEX IF NOT EXISTS idx_exercise_questions_exercise ON exercise_questions(exercise_id, position);

-- 4) Lượt làm bài
CREATE TABLE IF NOT EXISTS exercise_attempts (
    attempt_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exercise_id UUID NOT NULL REFERENCES online_exercises(exercise_id) ON DELETE CASCADE ON UPDATE CASCADE,
    learner_id UUID NOT NULL REFERENCES learners(learner_id) ON DELETE CASCADE ON UPDATE CASCADE,
    attempt_number INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    deadline_at TIMESTAMPTZ,                     -- started_at + thời gian làm bài; NULL = không giới hạn
    submitted_at TIMESTAMPTZ,
    submit_reason VARCHAR(10),                   -- MANUAL = học viên nộp, TIMEOUT = hết giờ, CLOSED = đóng đề
    status VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS',   -- IN_PROGRESS, SUBMITTED (chờ chấm), GRADED
    late BOOLEAN NOT NULL DEFAULT FALSE,         -- nộp sau giờ đóng đề
    auto_score NUMERIC(8, 2),
    final_score NUMERIC(8, 2),
    max_score NUMERIC(8, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exercise_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    CONSTRAINT uq_exercise_attempt_number UNIQUE (exercise_id, learner_id, attempt_number)
);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_exercise ON exercise_attempts(exercise_id, learner_id);

-- 5) Câu trả lời
CREATE TABLE IF NOT EXISTS exercise_answers (
    answer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL REFERENCES exercise_attempts(attempt_id) ON DELETE CASCADE ON UPDATE CASCADE,
    question_id UUID NOT NULL REFERENCES exercise_questions(question_id) ON DELETE CASCADE ON UPDATE CASCADE,
    response TEXT,                               -- JSON câu trả lời của học viên
    correct BOOLEAN,                             -- NULL = câu tự luận chờ gia sư chấm
    auto_score NUMERIC(6, 2),
    tutor_score NUMERIC(6, 2),                   -- điểm gia sư chấm / sửa (ưu tiên hơn điểm máy)
    tutor_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exercise_answer UNIQUE (attempt_id, question_id)
);

DROP TRIGGER IF EXISTS trg_online_exercises_updated_at ON online_exercises;
CREATE TRIGGER trg_online_exercises_updated_at BEFORE UPDATE ON online_exercises
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
DROP TRIGGER IF EXISTS trg_exercise_questions_updated_at ON exercise_questions;
CREATE TRIGGER trg_exercise_questions_updated_at BEFORE UPDATE ON exercise_questions
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
DROP TRIGGER IF EXISTS trg_exercise_attempts_updated_at ON exercise_attempts;
CREATE TRIGGER trg_exercise_attempts_updated_at BEFORE UPDATE ON exercise_attempts
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
DROP TRIGGER IF EXISTS trg_exercise_answers_updated_at ON exercise_answers;
CREATE TRIGGER trg_exercise_answers_updated_at BEFORE UPDATE ON exercise_answers
    FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
