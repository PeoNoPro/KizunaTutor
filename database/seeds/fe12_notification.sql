-- FE-12 Quản lý thông báo: TOÀN BỘ thay đổi database trong một file (gộp 3 file cũ ngày 2026-11-11).
-- Phần 1: hộp thông báo cá nhân (xóa mềm, thời điểm nhận) và tùy chọn nhận thông báo.
-- Phần 2: đợt gửi thông báo (Admin/Quản lý gửi, hẹn giờ, email) và kết quả từng người nhận.
-- Phần 3: đường dẫn trang liên quan cho thông báo tự động (yêu cầu hỗ trợ, sự cố, học thử, thanh toán, bài tập).
-- Đã chạy 3 file cũ rồi thì chạy file này cũng không sao (mọi lệnh đều kiểm tra đã có hay chưa).
-- Chạy trên DB đã có bảng gốc users, notifications, user_notifications và kiểu notification_type_enum.
-- Chạy lại được nhiều lần; không xóa dữ liệu, không chèn thông báo mẫu.
--
-- TRƯỚC KHI CHẠY, kiểm tra dữ liệu trùng (một người nhận cùng một thông báo nhiều lần).
-- Câu dưới phải trả 0 dòng; có dòng thì dừng lại và báo nhóm, không tự xóa:
--   SELECT notification_id, user_id, count(*) FROM user_notifications GROUP BY 1,2 HAVING count(*) > 1;
BEGIN;

SELECT pg_advisory_xact_lock(101201001);

DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM user_notifications GROUP BY notification_id, user_id HAVING count(*) > 1)
    THEN RAISE EXCEPTION 'FE12: user_notifications có bản trùng (cùng người, cùng thông báo); cần đối chiếu thủ công trước khi chạy'; END IF;
END $$;

-- Thời điểm thông báo đến tay từng người (dùng để sắp xếp) và xóa mềm khỏi danh sách cá nhân.
ALTER TABLE user_notifications ADD COLUMN IF NOT EXISTS received_at timestamptz;
ALTER TABLE user_notifications ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
-- Bản cũ chưa có thời điểm nhận thì lấy thời điểm tạo thông báo; không bịa thời điểm đọc.
UPDATE user_notifications un SET received_at = n.created_at
  FROM notifications n WHERE n.notification_id = un.notification_id AND un.received_at IS NULL;
ALTER TABLE user_notifications ALTER COLUMN received_at SET DEFAULT now();
ALTER TABLE user_notifications ALTER COLUMN received_at SET NOT NULL;

DO $$ BEGIN
  IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'uq_user_notifications_recipient')
    THEN ALTER TABLE user_notifications ADD CONSTRAINT uq_user_notifications_recipient UNIQUE (notification_id, user_id); END IF;
END $$;

-- Danh sách của một người: chưa xóa, mới nhất trước; và đếm chưa đọc.
CREATE INDEX IF NOT EXISTS idx_user_notif_inbox ON user_notifications(user_id, received_at DESC, user_notification_id DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_notif_unread_live ON user_notifications(user_id) WHERE deleted_at IS NULL AND is_read = false;

-- Tùy chọn nhận thông báo: một dòng phiên bản cho mỗi người (chống hai cửa sổ ghi đè nhau)
-- và một dòng cho mỗi nhóm thông báo. Thiếu dòng nghĩa là mặc định: bật trong ứng dụng, tắt email.
CREATE TABLE IF NOT EXISTS notification_preference_state (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id UUID NOT NULL REFERENCES notification_preference_state(user_id) ON DELETE CASCADE,
    notification_type notification_type_enum NOT NULL,
    in_app_enabled BOOLEAN NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    PRIMARY KEY (user_id, notification_type)
);

-- ===== Phần 2: đợt gửi thông báo và kết quả gửi =====
-- Mỗi đợt gửi: nội dung đã xác nhận (không sửa sau khi gửi), nhóm nhận, kênh, thời điểm hẹn/chốt và trạng thái tổng hợp.
CREATE TABLE IF NOT EXISTS notification_dispatches (
    dispatch_id UUID PRIMARY KEY,
    origin VARCHAR(10) NOT NULL CHECK (origin IN ('ADMIN','EVENT')),
    creator_id UUID REFERENCES users(user_id),
    submission_key UUID,
    event_key VARCHAR(255),
    notification_type notification_type_enum NOT NULL,
    title VARCHAR(150) NOT NULL CHECK (btrim(title) <> ''),
    content TEXT NOT NULL CHECK (btrim(content) <> '' AND char_length(content) <= 10000),
    urgent BOOLEAN NOT NULL DEFAULT false,
    target_mode VARCHAR(10) NOT NULL CHECK (target_mode IN ('ROLES','ALL','EXPLICIT')),
    target_roles TEXT[] NOT NULL DEFAULT '{}',
    channels TEXT[] NOT NULL CHECK (cardinality(channels) > 0 AND channels <@ ARRAY['IN_APP','EMAIL']),
    scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    frozen_at TIMESTAMPTZ,
    notification_id UUID REFERENCES notifications(notification_id),
    preview_count INTEGER,
    recipient_count INTEGER,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED','QUEUED','SENDING','COMPLETED','PARTIAL','FAILED','NO_RECIPIENTS','NEEDS_REVIEW')),
    CHECK (NOT urgent OR (target_mode = 'ALL' AND scheduled_at IS NULL AND 'IN_APP' = ANY(channels))),
    CHECK (origin <> 'ADMIN' OR (creator_id IS NOT NULL AND submission_key IS NOT NULL)),
    CHECK (origin <> 'EVENT' OR event_key IS NOT NULL)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_dispatch_submission ON notification_dispatches(creator_id, submission_key) WHERE submission_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_dispatch_event ON notification_dispatches(event_key) WHERE event_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notification_dispatch_due ON notification_dispatches(scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX IF NOT EXISTS idx_notification_dispatch_history ON notification_dispatches(created_at DESC, dispatch_id DESC);

-- Kết quả theo từng người nhận và từng kênh. Một người chỉ có một dòng cho mỗi kênh của một đợt (không gửi trùng).
CREATE TABLE IF NOT EXISTS notification_deliveries (
    delivery_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispatch_id UUID NOT NULL REFERENCES notification_dispatches(dispatch_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP','EMAIL')),
    state VARCHAR(20) NOT NULL CHECK (state IN ('IN_APP_AVAILABLE','PENDING','SENDING','EMAIL_ACCEPTED','FAILED','SKIPPED','UNKNOWN')),
    reason VARCHAR(30),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (dispatch_id, user_id, channel)
);
CREATE INDEX IF NOT EXISTS idx_notification_delivery_work ON notification_deliveries(next_attempt_at) WHERE state IN ('PENDING','SENDING');

-- ===== Phần 3: đường dẫn trang liên quan =====
-- Chỉ nhận đường dẫn nội bộ dạng "/..." (không nhận địa chỉ trang ngoài).
ALTER TABLE notification_dispatches ADD COLUMN IF NOT EXISTS action_path VARCHAR(300);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS action_path VARCHAR(300);
DO $$ BEGIN
  IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'ck_notification_dispatch_action_path')
    THEN ALTER TABLE notification_dispatches ADD CONSTRAINT ck_notification_dispatch_action_path CHECK (action_path IS NULL OR action_path ~ '^/[^/]'); END IF;
  IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'ck_notification_action_path')
    THEN ALTER TABLE notifications ADD CONSTRAINT ck_notification_action_path CHECK (action_path IS NULL OR action_path ~ '^/[^/]'); END IF;
END $$;

COMMIT;
