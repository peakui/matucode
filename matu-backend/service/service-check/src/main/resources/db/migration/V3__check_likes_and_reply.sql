-- V3: persist per-user check likes + add comment reply counters.
-- Idempotent so re-applying is harmless.

-- ---------------------------------------------------------------- check_record_likes
CREATE TABLE IF NOT EXISTS check_record_likes (
  id BIGINT NOT NULL PRIMARY KEY,
  check_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME NULL,
  UNIQUE KEY uk_check_user (check_id, user_id),
  KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------- check_comments.reply_count
SET @has_check_comment_reply_count := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'check_comments'
    AND column_name = 'reply_count'
);
SET @check_comment_reply_count_ddl := IF(
  @has_check_comment_reply_count = 0,
  'ALTER TABLE check_comments ADD COLUMN reply_count INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE check_comment_reply_count_stmt FROM @check_comment_reply_count_ddl;
EXECUTE check_comment_reply_count_stmt;
DEALLOCATE PREPARE check_comment_reply_count_stmt;

-- ---------------------------------------------------------------- check_comments.reply_to_user_id (guard in case entity/DB drifted)
SET @has_check_comment_reply_to := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'check_comments'
    AND column_name = 'reply_to_user_id'
);
SET @check_comment_reply_to_ddl := IF(
  @has_check_comment_reply_to = 0,
  'ALTER TABLE check_comments ADD COLUMN reply_to_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE check_comment_reply_to_stmt FROM @check_comment_reply_to_ddl;
EXECUTE check_comment_reply_to_stmt;
DEALLOCATE PREPARE check_comment_reply_to_stmt;
