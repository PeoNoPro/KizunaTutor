BEGIN;
CREATE TABLE IF NOT EXISTS staff_role_change_requests (
    request_id UUID PRIMARY KEY,
    target_user_id UUID NOT NULL REFERENCES users(user_id),
    requester_user_id UUID NOT NULL REFERENCES users(user_id),
    from_role_id SMALLINT NOT NULL REFERENCES roles(role_id),
    to_role_id SMALLINT NOT NULL REFERENCES roles(role_id),
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    decided_by UUID REFERENCES users(user_id),
    decision_note VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    CONSTRAINT staff_role_change_not_self CHECK (target_user_id <> requester_user_id),
    CONSTRAINT staff_role_change_decided CHECK ((status = 'PENDING') = (decided_by IS NULL AND decided_at IS NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_role_change_pending ON staff_role_change_requests(target_user_id) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_staff_role_change_list ON staff_role_change_requests(status, created_at DESC, request_id);
CREATE INDEX IF NOT EXISTS idx_staff_role_change_requester ON staff_role_change_requests(requester_user_id, created_at DESC, request_id);
COMMIT;
