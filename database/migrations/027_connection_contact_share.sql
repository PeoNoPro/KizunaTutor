-- Chia sẻ liên hệ giữa học viên và gia sư.
--   * Học viên gửi yêu cầu (học thử hoặc kết nối khóa học): đồng ý gửi hồ sơ + email + số điện thoại cho gia sư.
--     Lưu bản sao liên hệ theo TỪNG yêu cầu (học viên đổi số sau này thì yêu cầu cũ vẫn đúng số đã gửi).
--   * Gia sư đồng ý (học thử được nhận / kết nối khóa học): học viên thấy email + số điện thoại của gia sư.
--   * Yêu cầu bị từ chối / hủy / hết hạn: ẩn liên hệ ở cả hai phía. Khóa học đã kết thúc vẫn giữ.
-- Học thử dùng chung bảng connection_requests (yêu cầu loại TRIAL) nên áp dụng cho cả hai loại.
-- Chạy SAU 026_connection_course_finalize.sql. Idempotent.

ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20);
-- Thời điểm học viên đồng ý chia sẻ liên hệ (bằng chứng đồng ý theo Nghị định 13/2023/NĐ-CP)
ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS contact_consent_at TIMESTAMPTZ;
