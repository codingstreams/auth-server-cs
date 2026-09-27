-- Migration: V3__add_avatar_seed_to_app_user.sql
-- Description: Add avatar_seed column to app_user table to persist DiceBear avatar seed

ALTER TABLE app_user
    ADD COLUMN avatar_seed VARCHAR(100);
