-- ================================================================
-- apilens_files — V1
-- File metadata (binary content lives in MinIO, keyed by storage_key)
-- ================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE files (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id           UUID NOT NULL,
    owner_id         UUID NOT NULL,
    original_name    VARCHAR(500) NOT NULL,
    storage_key      VARCHAR(500) NOT NULL UNIQUE,
    content_type     VARCHAR(150),
    size_bytes       BIGINT NOT NULL,
    checksum_sha256  VARCHAR(64),
    status           VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT files_size_bytes_check CHECK (size_bytes >= 0),
    CONSTRAINT files_status_check CHECK (status IN ('UPLOADING','AVAILABLE','INFECTED','DELETED'))
);

CREATE INDEX idx_files_org ON files(org_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_files_owner ON files(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_files_status ON files(status);
