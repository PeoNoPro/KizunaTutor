-- Sửa theo góp ý review PR #21 (2026-11-11). Chạy SAU fe11_operations_schema.sql và fe12_notification.sql.
-- Chạy lại được nhiều lần; không xóa dữ liệu.
--   1. Nhật ký nhân sự có hành động riêng DELETE (xóa tài khoản), thay cho STATUS + chữ "Xóa tài khoản:".
--   2. Sự cố kỹ thuật gắn với yêu cầu hỗ trợ bằng cột support_ticket_id (không dò chuỗi trong mô tả nữa).
--   3. Hủy thông báo đã hẹn giờ: trạng thái CANCELLED, ai hủy, lúc nào (hiện trong Nhật ký thao tác).
--   4. Đường dẫn trong thông báo chỉ nhận ký tự an toàn (chặn dạng /\trang-ngoai).
BEGIN;

SELECT pg_advisory_xact_lock(101101021);

-- 1. Hành động DELETE cho nhật ký nhân sự; đổi các dòng xóa cũ sang DELETE và bỏ tiền tố.
ALTER TABLE staff_account_audit_events DROP CONSTRAINT IF EXISTS staff_account_audit_events_action_check;
ALTER TABLE staff_account_audit_events ADD CONSTRAINT staff_account_audit_events_action_check
    CHECK (action IN ('CREATE','UPDATE','ROLE','STATUS','DELETE'));
UPDATE staff_account_audit_events
   SET action = 'DELETE', reason = btrim(substring(reason FROM char_length('Xóa tài khoản:') + 1))
 WHERE action = 'STATUS' AND reason LIKE 'Xóa tài khoản:%';

-- 2. Sự cố gắn với yêu cầu hỗ trợ; điền sẵn cho sự cố cũ từ dòng "Mã yêu cầu hỗ trợ: <mã>" trong mô tả.
ALTER TABLE technical_incidents ADD COLUMN IF NOT EXISTS support_ticket_id UUID;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_technical_incident_support_ticket') THEN
    ALTER TABLE technical_incidents ADD CONSTRAINT fk_technical_incident_support_ticket
      FOREIGN KEY (support_ticket_id) REFERENCES support_tickets(ticket_id) ON DELETE SET NULL;
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_technical_incident_support_ticket ON technical_incidents(support_ticket_id, reporter_id)
    WHERE support_ticket_id IS NOT NULL;
UPDATE technical_incidents i
   SET support_ticket_id = t.ticket_id
  FROM support_tickets t
 WHERE i.support_ticket_id IS NULL
   AND t.ticket_id::text = lower(substring(i.description FROM 'Mã yêu cầu hỗ trợ:\s*([0-9a-fA-F-]{36})'));

-- 3. Hủy thông báo hẹn giờ.
ALTER TABLE notification_dispatches ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE notification_dispatches ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES users(user_id);
ALTER TABLE notification_dispatches DROP CONSTRAINT IF EXISTS notification_dispatches_status_check;
ALTER TABLE notification_dispatches ADD CONSTRAINT notification_dispatches_status_check
    CHECK (status IN ('SCHEDULED','QUEUED','SENDING','COMPLETED','PARTIAL','FAILED','NO_RECIPIENTS','NEEDS_REVIEW','CANCELLED'));
ALTER TABLE notification_dispatches DROP CONSTRAINT IF EXISTS ck_notification_dispatch_cancelled;
ALTER TABLE notification_dispatches ADD CONSTRAINT ck_notification_dispatch_cancelled
    CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL) AND (cancelled_at IS NULL OR (frozen_at IS NULL AND cancelled_by IS NOT NULL)));

-- 4. Đường dẫn nội bộ: bắt đầu bằng một dấu "/", không có "//", "\" hay ký tự điều khiển.
--    NOT VALID: chỉ kiểm dòng mới (dòng cũ đều do code nội bộ tạo, đúng dạng).
ALTER TABLE notification_dispatches DROP CONSTRAINT IF EXISTS ck_notification_dispatch_action_path;
ALTER TABLE notification_dispatches ADD CONSTRAINT ck_notification_dispatch_action_path
    CHECK (action_path IS NULL OR action_path ~ '^/([A-Za-z0-9_.~%=&?#+-][A-Za-z0-9/_.~%=&?#+-]*)?$') NOT VALID;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS ck_notification_action_path;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_action_path
    CHECK (action_path IS NULL OR action_path ~ '^/([A-Za-z0-9_.~%=&?#+-][A-Za-z0-9/_.~%=&?#+-]*)?$') NOT VALID;

COMMIT;
