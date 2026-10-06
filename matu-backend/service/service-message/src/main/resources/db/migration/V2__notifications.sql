-- V2: comment-reply notifications (and future system notices). Idempotent.

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '收件人',
  type VARCHAR(32) NOT NULL COMMENT 'comment_reply',
  source_type VARCHAR(16) NOT NULL COMMENT 'post | check | question',
  source_id BIGINT NOT NULL,
  source_title VARCHAR(255) NULL,
  comment_id BIGINT NULL,
  parent_comment_id BIGINT NULL,
  from_user_id BIGINT NOT NULL,
  from_nickname VARCHAR(64) NULL,
  from_avatar VARCHAR(512) NULL,
  content_preview VARCHAR(500) NULL,
  is_read TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NULL,
  KEY idx_user_read_created (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
