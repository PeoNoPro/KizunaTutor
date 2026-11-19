-- Kết nối khóa học theo môn: gia sư CHỐT số buổi + ngày bắt đầu, học viên XÁC NHẬN.
--   PENDING           : học viên gửi môn + thứ + giờ (đang giữ chỗ tạm)
--   AWAITING_LEARNER  : gia sư đã chốt số buổi / ngày bắt đầu, chờ học viên xác nhận (vẫn giữ chỗ)
--   ACCEPTED          : học viên xác nhận → khóa học chính thức
-- subject_level: mã môn (PriceLevel) học viên chọn; thời lượng mỗi buổi lấy theo bảng học phí của gia sư.
-- Chạy SAU 025_schedule_entries.sql.
-- Idempotent: chạy lại nhiều lần không lỗi.

ALTER TYPE connection_request_status_enum ADD VALUE IF NOT EXISTS 'AWAITING_LEARNER';

ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS subject_level VARCHAR(30);

-- Lịch chung chống trùng: "chờ học viên xác nhận" vẫn giữ chỗ
CREATE OR REPLACE FUNCTION schedule_status_of_connection(request_type TEXT, s TEXT) RETURNS VARCHAR AS $$
    SELECT CASE WHEN request_type <> 'DIRECT' THEN 'RELEASED'
                WHEN s IN ('PENDING', 'AWAITING_LEARNER') THEN 'HOLD'
                WHEN s = 'ACCEPTED' THEN 'CONFIRMED'
                ELSE 'RELEASED' END;
$$ LANGUAGE sql IMMUTABLE;
