-- Thông báo: thêm nhóm "Kết nối gia sư" (CONNECTION)
-- Mục đích: tách thông báo của luồng kết nối trực tiếp (gửi yêu cầu, gia sư đồng ý / từ chối, hủy, hết hạn,
-- đồng ý kết nối sau học thử) khỏi nhóm "Học thử" (TRIAL), để người dùng bật / tắt riêng trong cài đặt thông báo.
-- Chạy SAU database/seeds/fe12_notification.sql (bảng thông báo dùng kiểu notification_type_enum).
-- Idempotent: chạy lại nhiều lần không gây lỗi, không đổi dữ liệu đang có.

ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'CONNECTION';
