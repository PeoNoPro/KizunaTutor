-- FE-07: File nghe cho câu hỏi của bài làm trực tiếp — nội dung file lưu trên Cloudflare R2 (không lưu trong DB).
-- Chạy SAU migration 017_fe07_online_exercises (và các migration 018–032 của nhóm). Chạy lại nhiều lần vẫn an toàn.
-- Trước đây tên 031_fe07_exercise_audio (trùng số với 031_files_on_r2 và 032_practice_exam_mode_only của nhóm);
-- DB đã chạy bản cũ thì không cần chạy lại.
-- Không sửa bảng cũ.
--
-- exercise_audio: thông tin file gia sư tải lên; DB chỉ giữ storage_key (đường dẫn object trên R2, vd "audio/<uuid>.mp3").
-- - space_id: lớp của file (gia sư tải lên trước khi lưu bài nên lúc đầu chưa có exercise_id).
-- - exercise_id: gán khi lưu bài. Câu hỏi bỏ file / xóa bài → exercise_id = NULL (file "mồ côi");
--   file mồ côi quá 1 ngày được xóa cả trên R2 lẫn trong DB khi gia sư của lớp tải file mới.
-- - Câu hỏi giữ mã file + số lần nghe tối đa trong cột exercise_questions.data (JSON, khóa "audio").

CREATE TABLE IF NOT EXISTS exercise_audio (
    audio_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES learning_spaces(space_id) ON DELETE CASCADE ON UPDATE CASCADE,
    exercise_id UUID REFERENCES online_exercises(exercise_id) ON DELETE SET NULL ON UPDATE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exercise_audio_space ON exercise_audio(space_id);
CREATE INDEX IF NOT EXISTS idx_exercise_audio_exercise ON exercise_audio(exercise_id);
