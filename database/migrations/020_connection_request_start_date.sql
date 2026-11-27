-- Ngày bắt đầu học của yêu cầu kết nối trực tiếp
-- Học viên xem lịch rảnh theo tuần (tối đa 4 tuần) và chọn ca từ một tuần cụ thể.
-- start_date = ngày bắt đầu áp dụng các ca hằng tuần; dùng để kiểm tra trùng buổi học thử từ ngày đó trở đi.
-- NULL với yêu cầu cũ hoặc yêu cầu TRIAL (khi đó coi như bắt đầu từ ngày gia sư phản hồi).
-- Idempotent: chạy lại nhiều lần không gây lỗi.

ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS start_date DATE;
