-- V3: qa comment reply fields + index for "questions I liked". Idempotent.

-- ---------------------------------------------------------------- qa_comments.reply_to_user_id
SET @has_qa_comment_reply_to := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'qa_comments'
    AND column_name = 'reply_to_user_id'
);
SET @qa_comment_reply_to_ddl := IF(
  @has_qa_comment_reply_to = 0,
  'ALTER TABLE qa_comments ADD COLUMN reply_to_user_id BIGINT NULL',
  'SELECT 1'
);
PREPARE qa_comment_reply_to_stmt FROM @qa_comment_reply_to_ddl;
EXECUTE qa_comment_reply_to_stmt;
DEALLOCATE PREPARE qa_comment_reply_to_stmt;

-- ---------------------------------------------------------------- qa_comments.reply_count
SET @has_qa_comment_reply_count := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'qa_comments'
    AND column_name = 'reply_count'
);
SET @qa_comment_reply_count_ddl := IF(
  @has_qa_comment_reply_count = 0,
  'ALTER TABLE qa_comments ADD COLUMN reply_count INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE qa_comment_reply_count_stmt FROM @qa_comment_reply_count_ddl;
EXECUTE qa_comment_reply_count_stmt;
DEALLOCATE PREPARE qa_comment_reply_count_stmt;

-- ---------------------------------------------------------------- qa_votes (user_id,target_type,vote_type) index
SET @has_qa_votes_user_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'qa_votes'
    AND index_name = 'idx_user_target_vote'
);
SET @qa_votes_user_idx_ddl := IF(
  @has_qa_votes_user_idx = 0,
  'ALTER TABLE qa_votes ADD INDEX idx_user_target_vote (user_id, target_type, vote_type)',
  'SELECT 1'
);
PREPARE qa_votes_user_idx_stmt FROM @qa_votes_user_idx_ddl;
EXECUTE qa_votes_user_idx_stmt;
DEALLOCATE PREPARE qa_votes_user_idx_stmt;
