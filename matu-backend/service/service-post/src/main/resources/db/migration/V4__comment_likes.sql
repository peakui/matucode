-- V4: per-user likes on post comments. Idempotent so re-applying is harmless.

CREATE TABLE IF NOT EXISTS comment_likes (
  id BIGINT NOT NULL PRIMARY KEY,
  comment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME NULL,
  UNIQUE KEY uk_comment_user (comment_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
