-- Kết nối gia sư trực tiếp (không cần học thử)
-- Mục đích:
--   * connection_requests.request_type: 'TRIAL' (sinh ra từ đăng ký học thử, như trước) hoặc 'DIRECT'
--     (học viên gửi yêu cầu kết nối thẳng, chọn các ca hằng tuần trong lịch rảnh của gia sư).
--   * connection_request_slots: các ca hằng tuần học viên chọn cho yêu cầu DIRECT (lưu bản chụp giờ,
--     không tham chiếu tutor_availabilities để không mất dữ liệu khi gia sư sửa/xóa lịch rảnh).
--     day_of_week theo quy ước tutor_availabilities: 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy.
--   * connection_requests.response_message: lời nhắn / lý do từ chối của gia sư.
--   * Mỗi cặp học viên - gia sư chỉ có tối đa 1 yêu cầu DIRECT đang chờ duyệt.
-- Idempotent: chạy lại nhiều lần không gây lỗi, không xóa dữ liệu cũ (dữ liệu cũ mặc định là 'TRIAL').

ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS request_type VARCHAR(20) NOT NULL DEFAULT 'TRIAL';
-- Lời nhắn/lý do gia sư gửi kèm khi đồng ý hoặc từ chối yêu cầu kết nối trực tiếp
ALTER TABLE connection_requests ADD COLUMN IF NOT EXISTS response_message TEXT;

DO $$
BEGIN
    ALTER TABLE connection_requests
        ADD CONSTRAINT chk_connection_request_type CHECK (request_type IN ('TRIAL', 'DIRECT'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS connection_request_slots (
    slot_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES connection_requests(request_id) ON DELETE CASCADE ON UPDATE CASCADE,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT chk_conn_slot_dow CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_conn_slot_time CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_conn_slots_request ON connection_request_slots(request_id);
CREATE INDEX IF NOT EXISTS idx_conn_type_tutor_status ON connection_requests(tutor_id, request_type, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_conn_direct_pending
    ON connection_requests(learner_id, tutor_id)
    WHERE request_type = 'DIRECT' AND status = 'PENDING';
