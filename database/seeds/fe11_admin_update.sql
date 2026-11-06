-- Cập nhật FE-11: trả lại sự cố về danh sách chờ, hàng đợi email cấp tài khoản, lịch sử cấu hình dùng chung.
-- Chỉ thêm bảng và nới một ràng buộc, không sửa dữ liệu có sẵn; chạy lại không sao.
BEGIN;
ALTER TABLE technical_incident_events DROP CONSTRAINT IF EXISTS technical_incident_events_action_check;
ALTER TABLE technical_incident_events ADD CONSTRAINT technical_incident_events_action_check
    CHECK (action IN ('CREATE','CLAIM','REASSIGN','PROGRESS','RESOLVE','RESUME','CLOSE','RELEASE'));

-- Không có cột mật khẩu: mật khẩu tạm chỉ được sinh ngay lúc gửi thư.
CREATE TABLE IF NOT EXISTS staff_credential_emails (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    requested_by UUID NOT NULL REFERENCES users(user_id),
    attempts INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_staff_credential_emails_status ON staff_credential_emails(status, updated_at);

CREATE TABLE IF NOT EXISTS system_setting_audit_events (
    event_id UUID PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    actor_user_id UUID NOT NULL REFERENCES users(user_id),
    before_value VARCHAR(100) NOT NULL,
    after_value VARCHAR(100) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_system_setting_audit_time ON system_setting_audit_events(setting_key, occurred_at DESC, event_id);
COMMIT;
