-- V2: share counter for questions. Idempotent so re-applying is harmless.
SET @has_qa_share_count := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'qa_questions'
    AND column_name = 'share_count'
);

SET @qa_share_count_ddl := IF(
  @has_qa_share_count = 0,
  'ALTER TABLE qa_questions ADD COLUMN share_count INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE qa_share_count_stmt FROM @qa_share_count_ddl;
EXECUTE qa_share_count_stmt;
DEALLOCATE PREPARE qa_share_count_stmt;
