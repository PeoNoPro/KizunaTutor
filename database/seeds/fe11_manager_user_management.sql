-- FE-11 (2026-11-13): Quản lý tạm khóa / mở lại tài khoản Học viên và Gia sư.
-- Mỗi lần khóa/mở ghi một dòng (người làm, tài khoản, lý do, thời điểm); Admin xem trong Nhật ký hệ thống (nhóm "Người dùng").
-- Chạy được nhiều lần, không xóa dữ liệu.
BEGIN;

CREATE TABLE IF NOT EXISTS user_account_moderation_events (
    event_id        UUID PRIMARY KEY,
    actor_user_id   UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    target_user_id  UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    action          VARCHAR(16) NOT NULL CHECK (action IN ('SUSPEND', 'REACTIVATE')),
    reason          TEXT NOT NULL CHECK (char_length(btrim(reason)) BETWEEN 1 AND 500),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_moderation_target ON user_account_moderation_events(target_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_moderation_time ON user_account_moderation_events(occurred_at DESC, event_id DESC);

COMMIT;
