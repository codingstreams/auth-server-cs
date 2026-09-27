-- Migration: V2__add_provider_and_provider_id_to_app_user.sql
-- Description: Allow nullable password_hash for OAuth users and add provider and provider_id columns

-- 1. Make password_hash nullable for OAuth-authenticated users
ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;

-- 2. Create enum type for authentication providers
CREATE TYPE auth_provider AS ENUM ('local', 'google', 'github', 'LOCAL', 'GOOGLE', 'GITHUB');

-- 3. Add provider column with default 'local' and provider_id column
ALTER TABLE app_user
    ADD COLUMN provider auth_provider NOT NULL DEFAULT 'local',
    ADD COLUMN provider_id VARCHAR(255);

-- 4. Unique index for OAuth provider and provider_id lookups (partial index for non-null provider_id)
CREATE UNIQUE INDEX idx_app_user_provider_provider_id
    ON app_user (provider, provider_id)
    WHERE provider_id IS NOT NULL;
