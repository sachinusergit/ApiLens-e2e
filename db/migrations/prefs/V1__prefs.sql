-- ================================================================
-- apilens_prefs — V1
-- User preferences, confirmations, notifications, banners
-- ================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Slider preferences
CREATE TABLE preferences (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    org_id      UUID NOT NULL,
    key         VARCHAR(100) NOT NULL,
    value       INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT prefs_value_check CHECK (value >= 0 AND value <= 100)
);

CREATE UNIQUE INDEX prefs_user_key ON preferences(user_id, key);
CREATE INDEX idx_prefs_org ON preferences(org_id);

-- 2. Confirmations (Yes/No dialog history)
CREATE TABLE confirmations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    org_id      UUID NOT NULL,
    action      VARCHAR(100) NOT NULL,
    choice      VARCHAR(10) NOT NULL,
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT conf_choice_check CHECK (choice IN ('YES','NO'))
);

CREATE INDEX idx_conf_org ON confirmations(org_id, created_at DESC);

-- 3. Notifications (toast messages)
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    org_id      UUID NOT NULL,
    type        VARCHAR(20) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT notif_type_check CHECK (type IN ('success','error','info','warning'))
);

CREATE INDEX idx_notif_org ON notifications(org_id, is_read, created_at DESC);

-- 4. Marketing banners
CREATE TABLE banners (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(500),
    image_url   VARCHAR(500),
    cta_url     VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT true,
    priority    INTEGER NOT NULL DEFAULT 0,
    starts_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    ends_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_banners_active ON banners(is_active, priority DESC);

-- Seed 2 banners (only if the table is empty, so re-runs are safe)
INSERT INTO banners (title, body, image_url, cta_url, priority)
SELECT * FROM (VALUES
    ('Upgrade to Pro', 'Unlock advanced API testing features', 'https://via.placeholder.com/300x80?text=Pro+Plan', 'https://example.com/pro', 10),
    ('New Feature: Bulk Upload', 'Upload up to 100 files at once', 'https://via.placeholder.com/300x80?text=Bulk+Upload', 'https://example.com/bulk', 5)
) AS seed(title, body, image_url, cta_url, priority)
WHERE NOT EXISTS (SELECT 1 FROM banners);
