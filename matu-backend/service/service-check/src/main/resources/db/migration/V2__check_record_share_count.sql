-- V2: share counter for check-in records. Idempotent so re-applying is harmless.
SET @has_check_share_count := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'check_records'
    AND column_name = 'share_count'
);

SET @check_share_count_ddl := IF(
  @has_check_share_count = 0,
  'ALTER TABLE check_records ADD COLUMN share_count INT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE check_share_count_stmt FROM @check_share_count_ddl;
EXECUTE check_share_count_stmt;
DEALLOCATE PREPARE check_share_count_stmt;
