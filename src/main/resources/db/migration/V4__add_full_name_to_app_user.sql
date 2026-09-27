-- Migration: V4__add_full_name_to_app_user.sql
-- Description: Add full_name column to app_user table

ALTER TABLE app_user
    ADD COLUMN full_name VARCHAR(255);
