-- Migration: V1__create_app_user_and_authorities_tables.sql
-- Description: Create app_user and authorities tables with constraints and indexes

CREATE TABLE app_user (
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_blocked BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_app_user PRIMARY KEY (username)
);

CREATE TABLE authorities (
    username VARCHAR(100) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_app_user FOREIGN KEY (username) REFERENCES app_user (username) ON DELETE CASCADE
);

-- Unique index to prevent duplicate role assignments per user and optimize authority lookups
CREATE UNIQUE INDEX idx_authorities_username_authority ON authorities (username, authority);

-- Foreign key index on authorities.username for join and cascade performance
CREATE INDEX idx_authorities_username ON authorities (username);
