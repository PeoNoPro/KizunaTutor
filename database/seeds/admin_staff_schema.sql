BEGIN;
CREATE TABLE IF NOT EXISTS staff_account_audit_events (
    event_id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL REFERENCES users(user_id),
    target_user_id UUID NOT NULL REFERENCES users(user_id),
    action VARCHAR(30) NOT NULL CHECK (action IN ('CREATE','UPDATE','ROLE','STATUS')),
    reason VARCHAR(2000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_staff_account_audit_time ON staff_account_audit_events(occurred_at DESC,event_id DESC);
COMMIT;
SELECT version FROM operations_schema_versions;
SELECT to_regclass('public.staff_account_audit_events') IS NOT NULL AS co_bang_nhan_su;
SELECT to_regclass('public.fe10_schema_versions') IS NOT NULL AS co_fe10;
SELECT 'fe10_schema_versions' AS muc, to_regclass('public.fe10_schema_versions') IS NOT NULL AS da_co
UNION ALL SELECT 'ticket_escalations', to_regclass('public.ticket_escalations') IS NOT NULL
UNION ALL SELECT 'ticket_audit_events', to_regclass('public.ticket_audit_events') IS NOT NULL
UNION ALL SELECT 'ticket_attachments', to_regclass('public.ticket_attachments') IS NOT NULL
UNION ALL SELECT 'support_tickets.version', EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'support_tickets' AND column_name = 'version')
UNION ALL SELECT 'ticket_messages.sequence_no', EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'ticket_messages' AND column_name = 'sequence_no');