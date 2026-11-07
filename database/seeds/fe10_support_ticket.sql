-- FE-10: schema yêu cầu hỗ trợ, chuyển cấp, đính kèm và lịch ca.
-- Chạy trên DB đã có bảng nghiệp vụ gốc users, support_tickets, ticket_messages.
-- Chạy lại không xóa dữ liệu; không chứa dữ liệu mẫu.
BEGIN;

SELECT pg_advisory_xact_lock(101001026);
CREATE TABLE IF NOT EXISTS fe10_schema_versions(version integer PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now());
DO $$ BEGIN
  IF EXISTS(SELECT 1 FROM ticket_messages m LEFT JOIN support_tickets t ON t.ticket_id=m.ticket_id LEFT JOIN users u ON u.user_id=m.sender_id WHERE t.ticket_id IS NULL OR u.user_id IS NULL)
    THEN RAISE EXCEPTION 'FE10: orphan messages require manual reconciliation'; END IF;
  IF EXISTS(SELECT 1 FROM ticket_messages WHERE nullif(btrim(attachment_url),'') IS NOT NULL)
    THEN RAISE EXCEPTION 'FE10: reconcile legacy attachment URLs before migration'; END IF;
  IF NOT EXISTS(SELECT 1 FROM fe10_schema_versions WHERE version=1) AND EXISTS(SELECT 1 FROM support_tickets WHERE is_escalated OR escalated_to IS NOT NULL OR status::text='ESCALATED')
    THEN RAISE EXCEPTION 'FE10: legacy escalation requires verified history'; END IF;
END $$;
ALTER TABLE support_tickets ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
ALTER TABLE ticket_messages ADD COLUMN IF NOT EXISTS sequence_no bigint;
WITH ranked AS (SELECT message_id,row_number() OVER(PARTITION BY ticket_id ORDER BY sent_at,message_id) AS seq FROM ticket_messages)
UPDATE ticket_messages m SET sequence_no=r.seq FROM ranked r WHERE m.message_id=r.message_id AND m.sequence_no IS NULL;
ALTER TABLE ticket_messages ALTER COLUMN sequence_no SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_messages_sequence ON ticket_messages(ticket_id,sequence_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_messages_identity ON ticket_messages(ticket_id,message_id);
CREATE TABLE IF NOT EXISTS ticket_escalations(
 escalation_id uuid PRIMARY KEY, ticket_id uuid NOT NULL REFERENCES support_tickets(ticket_id),
 referred_by uuid NOT NULL REFERENCES users(user_id), current_manager_id uuid NOT NULL REFERENCES users(user_id),
 reason text NOT NULL CHECK(char_length(btrim(reason)) BETWEEN 1 AND 10000),
 handling_summary text NOT NULL CHECK(char_length(btrim(handling_summary)) BETWEEN 1 AND 10000),
 created_at timestamptz NOT NULL DEFAULT now(), returned_at timestamptz,
 pending boolean NOT NULL DEFAULT true, recipient_revision bigint NOT NULL DEFAULT 0 CHECK(recipient_revision>=0),
 decision_text text CHECK(decision_text IS NULL OR char_length(btrim(decision_text)) BETWEEN 1 AND 10000),
 decision_by uuid REFERENCES users(user_id), decision_revision bigint, decided_at timestamptz
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_escalations_pending ON ticket_escalations(ticket_id) WHERE pending;
CREATE INDEX IF NOT EXISTS idx_ticket_escalations_created ON ticket_escalations(ticket_id,created_at DESC);
CREATE TABLE IF NOT EXISTS ticket_audit_events(
 event_id uuid PRIMARY KEY, ticket_id uuid NOT NULL REFERENCES support_tickets(ticket_id),
 actor_user_id uuid NOT NULL REFERENCES users(user_id), event_type varchar(40) NOT NULL,
 occurred_at timestamptz NOT NULL DEFAULT now(), ticket_version bigint NOT NULL,
 before_values jsonb NOT NULL, after_values jsonb NOT NULL, note text
);
CREATE INDEX IF NOT EXISTS idx_ticket_audit_events_time ON ticket_audit_events(ticket_id,occurred_at,event_id);
CREATE TABLE IF NOT EXISTS ticket_attachments(
 attachment_id uuid PRIMARY KEY,ticket_id uuid NOT NULL REFERENCES support_tickets(ticket_id),message_id uuid NOT NULL,
 uploaded_by uuid NOT NULL REFERENCES users(user_id),original_name varchar(255) NOT NULL,
 content_type varchar(80) NOT NULL CHECK(content_type IN ('image/jpeg','image/png','application/pdf')),
 size_bytes bigint NOT NULL CHECK(size_bytes BETWEEN 1 AND 10000000),created_at timestamptz NOT NULL DEFAULT now(),content bytea NOT NULL,
 CONSTRAINT fk_ticket_attachments_message FOREIGN KEY(ticket_id,message_id) REFERENCES ticket_messages(ticket_id,message_id),
 CONSTRAINT ck_ticket_attachments_size CHECK(octet_length(content)=size_bytes)
);
CREATE INDEX IF NOT EXISTS idx_ticket_attachments_message ON ticket_attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_support_tickets_owner_time ON support_tickets(user_id,updated_at DESC,ticket_id DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_assignee_status ON support_tickets(assigned_to,status,updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_recipient_time ON support_tickets(escalated_to,updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_tickets_queue ON support_tickets(status,category,priority,updated_at DESC);
INSERT INTO fe10_schema_versions(version) VALUES(1) ON CONFLICT DO NOTHING;

-- PHÂN CA NHÂN VIÊN HỖ TRỢ
-- FE-10: Phân ca nhân viên hỗ trợ (CS). Quản lý xếp nhân viên vào ca; hệ thống tự giao yêu cầu
-- cho nhân viên đang trong ca và không còn yêu cầu đang xử lý. Chạy lại nhiều lần không lỗi.
-- Giờ ca (giờ Việt Nam): MORNING 08:00-12:00, AFTERNOON 13:00-17:00, EVENING 18:00-22:00.

CREATE TABLE IF NOT EXISTS support_shifts (
    shift_id    uuid PRIMARY KEY,
    staff_id    uuid NOT NULL REFERENCES users(user_id),
    shift_date  date NOT NULL,
    shift_code  varchar(10) NOT NULL CHECK (shift_code IN ('MORNING', 'AFTERNOON', 'EVENING')),
    created_by  uuid NOT NULL REFERENCES users(user_id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    UNIQUE (staff_id, shift_date, shift_code)
);
CREATE INDEX IF NOT EXISTS idx_support_shifts_date ON support_shifts (shift_date, shift_code);
CREATE INDEX IF NOT EXISTS idx_support_shifts_staff_date ON support_shifts (staff_id, shift_date);

-- LỊCH SỬ XẾP/GỠ CA (chỉ thêm, không sửa, không xóa)
-- Mỗi lần Quản lý xếp hoặc gỡ một ca được ghi một dòng, hiện ở "Nhật ký thao tác" của Quản trị viên.
-- Gỡ ca vẫn xóa dòng trong support_shifts, nhưng dấu vết còn ở đây.
CREATE TABLE IF NOT EXISTS support_shift_events (
    event_id    uuid PRIMARY KEY,
    shift_id    uuid NOT NULL,
    staff_id    uuid NOT NULL REFERENCES users(user_id),
    shift_date  date NOT NULL,
    shift_code  varchar(10) NOT NULL CHECK (shift_code IN ('MORNING', 'AFTERNOON', 'EVENING')),
    action      varchar(10) NOT NULL CHECK (action IN ('ASSIGN', 'REMOVE')),
    actor_id    uuid NOT NULL REFERENCES users(user_id),
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_support_shift_events_time ON support_shift_events (occurred_at DESC);

CREATE OR REPLACE FUNCTION support_shift_events_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Lịch sử xếp ca chỉ được thêm, không được sửa hoặc xóa.';
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_support_shift_events_append_only ON support_shift_events;
CREATE TRIGGER trg_support_shift_events_append_only BEFORE UPDATE OR DELETE ON support_shift_events
    FOR EACH ROW EXECUTE FUNCTION support_shift_events_append_only();

-- Ca đã xếp trước khi có bảng lịch sử: ghi bù một dòng "xếp ca" theo người tạo và thời điểm tạo.
INSERT INTO support_shift_events(event_id, shift_id, staff_id, shift_date, shift_code, action, actor_id, occurred_at)
SELECT gen_random_uuid(), s.shift_id, s.staff_id, s.shift_date, s.shift_code, 'ASSIGN', s.created_by, s.created_at
FROM support_shifts s
WHERE NOT EXISTS (SELECT 1 FROM support_shift_events e WHERE e.shift_id = s.shift_id AND e.action = 'ASSIGN');

COMMIT;
