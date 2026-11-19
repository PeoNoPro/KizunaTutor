-- FE-07: Ảnh minh họa cho câu hỏi của bài làm trực tiếp.
-- Chạy SAU 031_files_on_r2 và 033_fe07_exercise_audio (trước đây tên 032_fe07_exercise_image; DB đã chạy bản cũ thì không cần chạy lại). Chạy lại nhiều lần vẫn an toàn. Không sửa bảng cũ.
--
-- Lưu nội dung giống các tệp khác của nhóm (031_files_on_r2):
-- - Máy chủ bật R2 (app.r2.enabled=true): ảnh lên Cloudflare R2 với khóa "exercise-images/<image_id>", cột image_data để NULL.
-- - Không bật R2: ảnh lưu trong cột image_data (BYTEA).
-- space_id: lớp của ảnh (gia sư tải ảnh lên trước khi lưu bài nên lúc đầu chưa có exercise_id).
-- exercise_id: gán khi lưu bài. Câu hỏi bỏ ảnh / xóa bài → exercise_id = NULL; ảnh mồ côi quá 1 ngày được xóa
-- (cả trên R2) khi gia sư của lớp tải ảnh mới. Câu hỏi giữ mã ảnh trong exercise_questions.data (JSON, khóa "image").

CREATE TABLE IF NOT EXISTS exercise_image (
    image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES learning_spaces(space_id) ON DELETE CASCADE ON UPDATE CASCADE,
    exercise_id UUID REFERENCES online_exercises(exercise_id) ON DELETE SET NULL ON UPDATE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    image_data BYTEA,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exercise_image_space ON exercise_image(space_id);
CREATE INDEX IF NOT EXISTS idx_exercise_image_exercise ON exercise_image(exercise_id);
