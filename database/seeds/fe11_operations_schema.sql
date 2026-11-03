-- ====================================================================================================
-- FE-11: QUẢN TRỊ HỆ THỐNG & BÁO CÁO VẬN HÀNH
-- (Cấu hình học thử, sự cố kỹ thuật, điều khoản & chính sách)
-- Không DROP các bảng dưới đây: chạy lại file không làm mất lịch sử cấu hình và sự cố.
-- Cần có sẵn: users, system_configurations (2 khóa TRIAL_DEFAULT_DURATION_MINUTES,
-- BOOKING_MIN_HOURS_IN_ADVANCE là số nguyên hợp lệ), trial_bookings.
-- Đây là nguồn schema FE-11 duy nhất; test FE-11 chạy toàn bộ file này.
-- ====================================================================================================
BEGIN;
SELECT pg_advisory_xact_lock(11001001);

-- 1. BẢNG ĐÁNH DẤU PHIÊN BẢN SCHEMA FE-11
CREATE TABLE IF NOT EXISTS operations_schema_versions (
    version VARCHAR(50) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Kiểm tra cấu hình học thử hiện có trước khi tạo bản cấu hình gốc
DO $$
DECLARE duration_text TEXT; advance_text TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM operations_schema_versions WHERE version = 'fe11_001') THEN
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
WHERE NOT EXISTS (SELECT 1 FROM operations_schema_versions WHERE version = 'fe11_001')
  AND NOT EXISTS (SELECT 1 FROM booking_policy_revisions);
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

-- 5. BẢNG SỰ CỐ KỸ THUẬT
CREATE TABLE IF NOT EXISTS technical_incidents (
    incident_id UUID PRIMARY KEY,
    subject VARCHAR(200) NOT NULL CHECK (length(btrim(subject)) > 0),
    description TEXT NOT NULL CHECK (length(btrim(description)) BETWEEN 1 AND 10000),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    reporter_id UUID NOT NULL REFERENCES users(user_id),
    assignee_id UUID REFERENCES users(user_id),
    recurrence_of UUID REFERENCES technical_incidents(incident_id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_incidents_status_severity_created ON technical_incidents(status, severity, created_at, incident_id);
CREATE INDEX IF NOT EXISTS idx_incidents_assignee ON technical_incidents(assignee_id, status);

-- 6. BẢNG THÀNH PHẦN BỊ ẢNH HƯỞNG CỦA SỰ CỐ
CREATE TABLE IF NOT EXISTS incident_components (
    incident_id UUID NOT NULL REFERENCES technical_incidents(incident_id),
    component_code VARCHAR(30) NOT NULL CHECK (component_code IN ('APPLICATION', 'DATABASE', 'AGENT_MEMORY')),
    PRIMARY KEY (incident_id, component_code)
);

-- 7. BẢNG LỊCH SỬ XỬ LÝ SỰ CỐ
CREATE TABLE IF NOT EXISTS technical_incident_events (
    event_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES technical_incidents(incident_id),
    sequence_no BIGINT NOT NULL CHECK (sequence_no > 0),
    actor_id UUID NOT NULL REFERENCES users(user_id),
    occurred_at TIMESTAMPTZ NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'CLAIM', 'REASSIGN', 'PROGRESS', 'RESOLVE', 'RESUME', 'CLOSE')),
    previous_state JSONB,
    new_state JSONB NOT NULL,
    detail TEXT CHECK (detail IS NULL OR length(btrim(detail)) BETWEEN 1 AND 10000),
    reason VARCHAR(2000) CHECK (reason IS NULL OR length(btrim(reason)) BETWEEN 1 AND 2000),
    verification_text TEXT CHECK (verification_text IS NULL OR length(btrim(verification_text)) BETWEEN 1 AND 10000),
    UNIQUE (incident_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS idx_events_action_time ON technical_incident_events(action, occurred_at, incident_id);

-- 8. BẢNG PHIÊN BẢN ĐIỀU KHOẢN & CHÍNH SÁCH
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

INSERT INTO operations_schema_versions (version) VALUES ('fe11_001') ON CONFLICT DO NOTHING;
COMMIT;
