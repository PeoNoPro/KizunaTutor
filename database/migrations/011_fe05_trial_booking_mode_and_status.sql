-- FE-05 Trial Booking & Connection Management
-- Mục đích: Bổ sung giá trị DECLINED, EXPIRED vào enum trial_booking_status_enum
-- và bổ sung cột learning_mode, location_details vào bảng trial_bookings.
-- Idempotent: chạy an toàn, không xóa dữ liệu cũ.

ALTER TYPE trial_booking_status_enum ADD VALUE IF NOT EXISTS 'DECLINED';
ALTER TYPE trial_booking_status_enum ADD VALUE IF NOT EXISTS 'EXPIRED';

ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS learning_mode VARCHAR(20) DEFAULT 'ONLINE';
ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS location_details TEXT;

