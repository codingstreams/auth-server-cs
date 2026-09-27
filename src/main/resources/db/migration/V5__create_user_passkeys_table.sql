-- Migration: V5__create_user_passkeys_table.sql
-- Description: Create user_passkeys table for WebAuthn passkey credentials
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE user_passkeys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(100) NOT NULL,
    credential_id VARCHAR(512) NOT NULL UNIQUE,
    public_key_cose BYTEA NOT NULL,
    sign_count BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_passkeys_app_user
        FOREIGN KEY (user_id)
        REFERENCES app_user (username)
        ON DELETE CASCADE
);

-- Index on the Foreign Key for optimized query performance
CREATE INDEX idx_user_passkeys_user_id ON user_passkeys(user_id);
