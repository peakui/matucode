-- V3: allow post comments to record the user being replied to. Idempotent so re-applying is harmless.
SET @has_comment_reply_to := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND column_name = 'reply_to_user_id'
);

SET @comment_reply_to_ddl := IF(
  @has_comment_reply_to = 0,
  'ALTER TABLE comments ADD COLUMN reply_to_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE comment_reply_to_stmt FROM @comment_reply_to_ddl;
EXECUTE comment_reply_to_stmt;
DEALLOCATE PREPARE comment_reply_to_stmt;
