-- V1: add mini-program identity and account deletion tables.
-- init.sql remains the Docker first-install dump; this migration is for every managed schema.
CREATE TABLE IF NOT EXISTS user_external_identities (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  provider VARCHAR(32) NOT NULL,
  app_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  open_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  union_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_provider_open (provider, app_id, open_id),
  UNIQUE KEY uk_provider_user (provider, app_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_deletion_requests (
  user_id BIGINT NOT NULL PRIMARY KEY,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  created_at DATETIME NOT NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rollback application first. Retain these tables until binding/deletion data is exported.
