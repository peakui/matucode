-- Durable AI chat records (local MySQL `matu_ai`).
--
-- Managed by a second, programmatic Flyway in AiRecordStore, scoped to its own
-- private Hikari pool -- NOT by the primary Flyway, which stays on PostgreSQL
-- (pgvector RAG, locations classpath:db/migration).
--
-- This location is deliberately a sibling of db/migration, not a subdirectory:
-- the primary Flyway scans classpath:db/migration recursively, so MySQL DDL
-- placed underneath it would both collide on version numbers and be executed
-- against PostgreSQL.
--
-- Tables are also created defensively with IF NOT EXISTS because the service
-- baselines this schema at version 0 (see AiRecordStore), so V1 can run against
-- a database whose tables were created by an earlier release.

CREATE TABLE IF NOT EXISTS ai_conversation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(100),
  scene VARCHAR(50),
  message_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_conversation (user_id, conversation_id),
  KEY idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_conversation (user_id, conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
