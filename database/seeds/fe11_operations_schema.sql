-- FE-11: schema quản trị, nhân sự, email và audit cấu hình.
-- Một transaction cho toàn bộ thay đổi; không chứa tài khoản hoặc giao dịch mẫu.
BEGIN;

-- ====================================================================================================
-- FE-11: QUẢN TRỊ HỆ THỐNG & BÁO CÁO VẬN HÀNH
-- (Cấu hình học thử, điều khoản & chính sách)
-- Không DROP các bảng dưới đây: chạy lại file không làm mất lịch sử cấu hình.
-- Cần có sẵn: users, system_configurations (2 khóa TRIAL_DEFAULT_DURATION_MINUTES,
-- BOOKING_MIN_HOURS_IN_ADVANCE là số nguyên hợp lệ), trial_bookings.
-- Đây là nguồn schema FE-11 duy nhất; test FE-11 chạy toàn bộ file này.
-- ====================================================================================================

SELECT pg_advisory_xact_lock(11001001);

-- Kiểm tra cấu hình học thử hiện có trước khi tạo bản cấu hình gốc
DO $$
DECLARE duration_text TEXT; advance_text TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM booking_policy_revisions) THEN
        SELECT config_value INTO duration_text FROM system_configurations WHERE config_key = 'TRIAL_DEFAULT_DURATION_MINUTES';
        SELECT config_value INTO advance_text FROM system_configurations WHERE config_key = 'BOOKING_MIN_HOURS_IN_ADVANCE';
        IF duration_text IS NULL OR advance_text IS NULL OR duration_text !~ '^[0-9]{1,3}$' OR advance_text !~ '^[0-9]{1,3}$' THEN
            RAISE EXCEPTION 'FE11: approved integer configuration is missing or invalid; migration stopped';
        END IF;
        IF duration_text::INTEGER NOT BETWEEN 15 AND 120 OR advance_text::INTEGER NOT BETWEEN 1 AND 168 THEN
            RAISE EXCEPTION 'FE11: configuration outside approved bounds; migration stopped';
        END IF;
    END IF;
END $$;

-- 2. BẢNG LỊCH SỬ CẤU HÌNH HỌC THỬ
CREATE TABLE IF NOT EXISTS booking_policy_revisions (
    revision_id UUID PRIMARY KEY,
    revision_number BIGINT NOT NULL UNIQUE,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 15 AND 120),
    min_advance_hours INTEGER NOT NULL CHECK (min_advance_hours BETWEEN 1 AND 168),
    effective_at TIMESTAMPTZ NOT NULL,
    changed_by UUID REFERENCES users(user_id),
    change_reason VARCHAR(2000),
    provenance VARCHAR(20) NOT NULL CHECK (provenance IN ('BASELINE', 'ADMIN')),
    CHECK ((provenance = 'BASELINE' AND changed_by IS NULL) OR
           (provenance = 'ADMIN' AND changed_by IS NOT NULL AND length(btrim(change_reason)) BETWEEN 1 AND 2000))
);

-- 3. BẢNG CẤU HÌNH HỌC THỬ ĐANG ÁP DỤNG (CHỈ MỘT DÒNG)
CREATE TABLE IF NOT EXISTS booking_policy_states (
    state_id UUID PRIMARY KEY CHECK (state_id = '00000000-0000-0000-0000-000000000011'),
    current_revision_id UUID NOT NULL REFERENCES booking_policy_revisions(revision_id),
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. BẢNG NHẬT KÝ THAY ĐỔI CẤU HÌNH
CREATE TABLE IF NOT EXISTS configuration_audit_events (
    event_id UUID PRIMARY KEY,
    revision_id UUID NOT NULL REFERENCES booking_policy_revisions(revision_id),
    before_revision_id UUID NOT NULL REFERENCES booking_policy_revisions(revision_id),
    actor_id UUID NOT NULL REFERENCES users(user_id),
    occurred_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(2000) NOT NULL CHECK (length(btrim(reason)) BETWEEN 1 AND 2000)
);

-- Bản cấu hình gốc lấy từ system_configurations (chỉ tạo một lần)
INSERT INTO booking_policy_revisions (revision_id, revision_number, duration_minutes, min_advance_hours, effective_at, provenance)
SELECT gen_random_uuid(), 1,
    (SELECT config_value::INTEGER FROM system_configurations WHERE config_key = 'TRIAL_DEFAULT_DURATION_MINUTES'),
    (SELECT config_value::INTEGER FROM system_configurations WHERE config_key = 'BOOKING_MIN_HOURS_IN_ADVANCE'),
    clock_timestamp(), 'BASELINE'
WHERE NOT EXISTS (SELECT 1 FROM booking_policy_revisions);
INSERT INTO booking_policy_states (state_id, current_revision_id)
SELECT '00000000-0000-0000-0000-000000000011', revision_id FROM booking_policy_revisions WHERE revision_number = 1
ON CONFLICT (state_id) DO NOTHING;

-- Lượt đặt học thử lưu lại cấu hình tại thời điểm đặt
ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS policy_revision_id UUID REFERENCES booking_policy_revisions(revision_id);
ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS policy_duration_minutes INTEGER CHECK (policy_duration_minutes BETWEEN 15 AND 120);
ALTER TABLE trial_bookings ADD COLUMN IF NOT EXISTS policy_min_advance_hours INTEGER CHECK (policy_min_advance_hours BETWEEN 1 AND 168);
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'trial_bookings'::regclass AND conname = 'chk_fe11_booking_snapshot') THEN
        ALTER TABLE trial_bookings ADD CONSTRAINT chk_fe11_booking_snapshot CHECK (
            (policy_revision_id IS NULL AND policy_duration_minutes IS NULL AND policy_min_advance_hours IS NULL) OR
            (policy_revision_id IS NOT NULL AND policy_duration_minutes IS NOT NULL AND policy_min_advance_hours IS NOT NULL));
    END IF;
END $$;

-- 5. BẢNG PHIÊN BẢN ĐIỀU KHOẢN & CHÍNH SÁCH
CREATE TABLE IF NOT EXISTS policy_document_versions (
    document_version_id UUID PRIMARY KEY,
    document_type VARCHAR(30) NOT NULL CHECK (document_type IN ('terms', 'usage-policy', 'regulations')),
    title VARCHAR(200) NOT NULL CHECK (length(btrim(title)) > 0),
    version_label VARCHAR(100) NOT NULL CHECK (length(btrim(version_label)) > 0),
    approved_content TEXT NOT NULL CHECK (length(btrim(approved_content)) > 0),
    publication_state VARCHAR(20) NOT NULL CHECK (publication_state IN ('DRAFT', 'PUBLISHED', 'WITHDRAWN')),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    source_reference TEXT NOT NULL CHECK (length(btrim(source_reference)) > 0),
    approved_at TIMESTAMPTZ NOT NULL,
    CHECK (effective_to IS NULL OR effective_to > effective_from),
    UNIQUE (document_type, version_label)
);

CREATE INDEX IF NOT EXISTS idx_policy_effective ON policy_document_versions(document_type, publication_state, effective_from);

-- LỊCH SỬ QUẢN LÝ NHÂN SỰ
CREATE TABLE IF NOT EXISTS staff_account_audit_events (
    event_id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users(user_id),
    target_user_id UUID NOT NULL REFERENCES users(user_id),
    action VARCHAR(30) NOT NULL CHECK (action IN ('CREATE','UPDATE','ROLE','STATUS')),
    reason VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_staff_account_audit_time ON staff_account_audit_events(occurred_at DESC,event_id DESC);

-- HÀNG ĐỢI EMAIL VÀ LỊCH SỬ CẤU HÌNH
-- Hàng đợi email cấp tài khoản, lịch sử cấu hình dùng chung. Chỉ thêm bảng, chạy lại không sao.

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
