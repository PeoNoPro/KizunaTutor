-- Hồ sơ học viên: thêm địa chỉ (Tỉnh/Thành phố + địa chỉ chi tiết).
-- Địa chỉ học viên chỉ hiển thị cho chính học viên nên được phép ghi chi tiết (khác địa chỉ công khai của gia sư).
-- Người phụ trách tự chạy trên Supabase bằng DBeaver.
-- Idempotent: chạy lại nhiều lần không gây lỗi.

ALTER TABLE learners ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE learners ADD COLUMN IF NOT EXISTS address VARCHAR(255);
