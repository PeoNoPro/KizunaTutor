-- Hình thức học (online / trực tiếp) cho kết nối khóa học và lớp học.
--   * Mỗi yêu cầu kết nối và mỗi lớp chỉ có MỘT hình thức: ONLINE hoặc OFFLINE (gia sư dạy cả hai vẫn được,
--     học viên chọn một khi gửi yêu cầu). Mặc định ONLINE nên dữ liệu cũ giữ nguyên ý nghĩa.
--   * connection_requests.location_details: địa điểm học viên đề xuất khi chọn học trực tiếp.
--   * learning_spaces: link phòng học (lớp online) hoặc địa chỉ (lớp trực tiếp), gia sư sửa được trong lớp.
-- Chạy SAU 028_pending_requests_do_not_hold.sql. Idempotent.

ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS learning_mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS location_details TEXT;

ALTER TABLE learning_spaces ADD COLUMN IF NOT EXISTS learning_mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE learning_spaces ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(500);
ALTER TABLE learning_spaces ADD COLUMN IF NOT EXISTS location_details TEXT;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_connection_requests_learning_mode') THEN
    ALTER TABLE connection_requests ADD CONSTRAINT chk_connection_requests_learning_mode
      CHECK (learning_mode IN ('ONLINE', 'OFFLINE'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_learning_spaces_learning_mode') THEN
    ALTER TABLE learning_spaces ADD CONSTRAINT chk_learning_spaces_learning_mode
      CHECK (learning_mode IN ('ONLINE', 'OFFLINE'));
  END IF;
END $$;
