-- Hồ sơ học viên: thêm địa chỉ (Tỉnh/Thành phố + địa chỉ chi tiết).
-- Địa chỉ học viên chỉ hiển thị cho chính học viên nên được phép ghi chi tiết (khác địa chỉ công khai của gia sư).
-- Người phụ trách tự chạy trên Supabase bằng DBeaver.
-- Idempotent: chạy lại nhiều lần không gây lỗi.
-- Trước đây đặt tên 017_learner_address.sql (trùng số với 2 file 017 trên dev); đổi sang 030 cho rõ thứ tự.
-- Không phụ thuộc migration nào khác. Database đã chạy bản 017 thì không cần chạy lại.

ALTER TABLE learners ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE learners ADD COLUMN IF NOT EXISTS address VARCHAR(255);
