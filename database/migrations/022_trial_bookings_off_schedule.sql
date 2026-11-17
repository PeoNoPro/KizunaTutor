-- Đề xuất học thử ngoài lịch rảnh
-- Mục đích:
--   * Cách đặt học thử chính: học viên chọn giờ trống trong lịch rảnh của gia sư.
--   * Cách phụ "Đề xuất giờ khác": học viên tự chọn ngày giờ NGOÀI lịch rảnh; gia sư đồng ý hoặc từ chối.
--   * trial_bookings.is_off_schedule = TRUE đánh dấu yêu cầu là đề xuất ngoài lịch rảnh (server tự xác định),
--     dùng để hiển thị nhãn cho gia sư và giới hạn số đề xuất mỗi tuần.
-- Idempotent: chạy lại nhiều lần không gây lỗi.

ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS is_off_schedule BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_trial_off_schedule_learner_tutor
    ON trial_bookings(learner_id, tutor_id, created_at)
    WHERE is_off_schedule;
