-- Mở khóa công cụ học: mỗi lần học viên gọi API công cụ (bộ thẻ, ngữ pháp, từ vựng, chữa câu AI, minigame),
-- backend đếm lớp còn mở (ACTIVE/PAUSED) của học viên. Index này giữ truy vấn đó nhanh.
-- Idempotent: chạy lại nhiều lần không gây lỗi.

CREATE INDEX IF NOT EXISTS idx_learning_spaces_learner_status ON learning_spaces (learner_id, status);
