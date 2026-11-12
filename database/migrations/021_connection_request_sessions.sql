 -- Buổi học cụ thể của yêu cầu kết nối trực tiếp
-- Mục đích:
--   * Trước đây yêu cầu kết nối chỉ lưu MẪU ca hằng tuần (connection_request_slots: thứ + giờ), nên không biết
--     từng ngày cụ thể có bị trùng buổi học thử / buổi học của học viên khác hay không.
--   * connection_request_sessions: mỗi yêu cầu sinh ra 1–12 buổi học có ngày cụ thể (giờ Việt Nam).
--     Trạng thái buổi đi theo trạng thái yêu cầu: PENDING = chờ gia sư duyệt (giữ chỗ trong thời hạn phản hồi),
--     ACCEPTED = đã xác nhận; DECLINED / CANCELLED / EXPIRED = nhả chỗ.
--   * Bỏ cột connection_requests.start_date (020): ngày bắt đầu / kết thúc suy ra từ buổi đầu / buổi cuối.
--     Cột này chưa có dữ liệu khi gỡ bỏ.
-- Idempotent: chạy lại nhiều lần không gây lỗi.

CREATE TABLE IF NOT EXISTS connection_request_sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES connection_requests(request_id) ON DELETE CASCADE ON UPDATE CASCADE,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT chk_conn_session_time CHECK (start_time < end_time)
);

CREATE INDEX IF NOT EXISTS idx_conn_sessions_request ON connection_request_sessions(request_id);
CREATE INDEX IF NOT EXISTS idx_conn_sessions_date ON connection_request_sessions(session_date);

ALTER TABLE connection_requests DROP COLUMN IF EXISTS start_date;
