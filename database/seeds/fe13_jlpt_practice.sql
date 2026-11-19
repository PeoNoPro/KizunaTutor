-- FE-13 (2026-11-17): Luyện đề JLPT — ngân hàng đề của cán bộ học vụ và lượt luyện đề của học viên.
-- File nghe/ảnh KHÔNG lưu trong DB: practice_files chỉ giữ khóa file trong thư mục lưu trữ (sau này có thể là Cloudflare R2).
-- Chạy được nhiều lần, không xóa dữ liệu.
BEGIN;

CREATE TABLE IF NOT EXISTS practice_exams (
    exam_id            UUID PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    level              VARCHAR(2)   NOT NULL CHECK (level IN ('N1','N2','N3','N4','N5')),
    exam_month         DATE,
    source             VARCHAR(300),
    minutes_vocab      SMALLINT NOT NULL DEFAULT 0 CHECK (minutes_vocab BETWEEN 0 AND 300),
    minutes_grammar    SMALLINT NOT NULL DEFAULT 0 CHECK (minutes_grammar BETWEEN 0 AND 300),
    minutes_listening  SMALLINT NOT NULL DEFAULT 0 CHECK (minutes_listening BETWEEN 0 AND 300),
    status             VARCHAR(12)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','HIDDEN')),
    audio_file_id      UUID,
    created_by         UUID NOT NULL REFERENCES users(user_id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at       TIMESTAMPTZ,
    version            BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_practice_exams_level ON practice_exams(level, status);

CREATE TABLE IF NOT EXISTS practice_files (
    file_id        UUID PRIMARY KEY,
    exam_id        UUID NOT NULL REFERENCES practice_exams(exam_id),
    kind           VARCHAR(8)   NOT NULL CHECK (kind IN ('AUDIO','IMAGE')),
    storage_key    VARCHAR(300) NOT NULL UNIQUE,
    original_name  VARCHAR(255) NOT NULL,
    content_type   VARCHAR(80)  NOT NULL,
    size_bytes     BIGINT NOT NULL CHECK (size_bytes > 0),
    created_by     UUID NOT NULL REFERENCES users(user_id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_practice_files_exam ON practice_files(exam_id, kind);
DO $$ BEGIN
  ALTER TABLE practice_exams ADD CONSTRAINT fk_practice_exams_audio FOREIGN KEY (audio_file_id) REFERENCES practice_files(file_id);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS practice_mondai (
    mondai_id    UUID PRIMARY KEY,
    exam_id      UUID NOT NULL REFERENCES practice_exams(exam_id),
    section      VARCHAR(16) NOT NULL CHECK (section IN ('VOCAB','GRAMMAR_READING','LISTENING')),
    number       SMALLINT NOT NULL CHECK (number BETWEEN 1 AND 20),
    instruction  TEXT,
    sort_order   INT NOT NULL,
    UNIQUE (exam_id, section, number)
);

CREATE TABLE IF NOT EXISTS practice_groups (
    group_id       UUID PRIMARY KEY,
    mondai_id      UUID NOT NULL REFERENCES practice_mondai(mondai_id),
    code           VARCHAR(40) NOT NULL,
    passage        TEXT,
    image_file_id  UUID REFERENCES practice_files(file_id),
    sort_order     INT NOT NULL,
    UNIQUE (mondai_id, code)
);

CREATE TABLE IF NOT EXISTS practice_questions (
    question_id     UUID PRIMARY KEY,
    exam_id         UUID NOT NULL REFERENCES practice_exams(exam_id),
    mondai_id       UUID NOT NULL REFERENCES practice_mondai(mondai_id),
    group_id        UUID REFERENCES practice_groups(group_id),
    number          SMALLINT NOT NULL CHECK (number BETWEEN 1 AND 200),
    stem            TEXT,
    options         JSONB NOT NULL,
    correct         SMALLINT NOT NULL CHECK (correct BETWEEN 1 AND 4),
    explanation     TEXT,
    script          TEXT,
    image_file_id   UUID REFERENCES practice_files(file_id),
    audio_start_ms  INT CHECK (audio_start_ms >= 0),
    audio_end_ms    INT CHECK (audio_end_ms IS NULL OR audio_end_ms > audio_start_ms),
    sort_order      INT NOT NULL,
    UNIQUE (mondai_id, number)
);
CREATE INDEX IF NOT EXISTS idx_practice_questions_exam ON practice_questions(exam_id, sort_order);

CREATE TABLE IF NOT EXISTS practice_attempts (
    attempt_id      UUID PRIMARY KEY,
    learner_id      UUID NOT NULL REFERENCES users(user_id),
    level           VARCHAR(2) NOT NULL CHECK (level IN ('N1','N2','N3','N4','N5')),
    mode            VARCHAR(8) NOT NULL CONSTRAINT practice_attempts_mode_check CHECK (mode = 'EXAM'),
    exam_id         UUID REFERENCES practice_exams(exam_id),
    sections        TEXT NOT NULL,
    wrong_only      BOOLEAN NOT NULL DEFAULT false,
    question_count  INT NOT NULL CHECK (question_count > 0),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deadline_at     TIMESTAMPTZ,
    submitted_at    TIMESTAMPTZ,
    correct_count   INT
);
CREATE INDEX IF NOT EXISTS idx_practice_attempts_learner ON practice_attempts(learner_id, started_at DESC);

CREATE TABLE IF NOT EXISTS practice_attempt_answers (
    attempt_id   UUID NOT NULL REFERENCES practice_attempts(attempt_id),
    question_id  UUID NOT NULL REFERENCES practice_questions(question_id),
    position     INT NOT NULL,
    chosen       SMALLINT CHECK (chosen BETWEEN 1 AND 4),
    flagged      BOOLEAN NOT NULL DEFAULT false,
    is_correct   BOOLEAN,
    answered_at  TIMESTAMPTZ,
    PRIMARY KEY (attempt_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_practice_answers_question ON practice_attempt_answers(question_id);

COMMIT;
