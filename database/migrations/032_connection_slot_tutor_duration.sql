-- 032: Độ dài ca kết nối do GIA SƯ quyết định khi đồng ý.
-- Học viên chỉ gửi thứ + giờ bắt đầu → end_time để trống khi yêu cầu đang chờ;
-- gia sư bấm đồng ý thì chọn độ dài từng ca (60 / 90 / 120 phút) và server điền end_time.
-- Mọi truy vấn khóa lịch chỉ đọc end_time của yêu cầu ACCEPTED (luôn có giá trị).
-- Chạy lại nhiều lần vẫn an toàn.

ALTER TABLE connection_request_slots ALTER COLUMN end_time DROP NOT NULL;
