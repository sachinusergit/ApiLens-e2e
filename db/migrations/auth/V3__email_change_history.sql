-- ================================================================
-- apilens_auth — V3
-- Audit trail for email address changes (rate limited in Redis)
-- NOTE: V2 is already used by seed_demo_user.sql
-- ================================================================

CREATE TABLE email_change_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    old_email   VARCHAR(320) NOT NULL,
    new_email   VARCHAR(320) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500)
);

CREATE INDEX idx_email_change_user_time
    ON email_change_history(user_id, changed_at DESC);
