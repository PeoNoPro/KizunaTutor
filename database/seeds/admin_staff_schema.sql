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
