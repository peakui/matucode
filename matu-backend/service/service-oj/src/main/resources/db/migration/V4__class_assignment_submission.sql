-- V4: attribute OJ submissions to a class assignment so homework progress/ranking can be built.
-- Idempotent so re-applying is harmless. class_submissions already exists and is reused as-is.

-- ---------------------------------------------------------------- oj_submissions.assignment_id
SET @has_submission_assignment_id := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'oj_submissions'
    AND column_name = 'assignment_id'
);
SET @submission_assignment_id_ddl := IF(
  @has_submission_assignment_id = 0,
  'ALTER TABLE oj_submissions ADD COLUMN assignment_id BIGINT NULL COMMENT ''所属班级作业ID，空表示自由刷题''',
  'SELECT 1'
);
PREPARE submission_assignment_id_stmt FROM @submission_assignment_id_ddl;
EXECUTE submission_assignment_id_stmt;
DEALLOCATE PREPARE submission_assignment_id_stmt;

-- ---------------------------------------------------------------- oj_submissions idx_assignment_id
SET @has_submission_assignment_idx := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'oj_submissions'
    AND index_name = 'idx_assignment_id'
);
SET @submission_assignment_idx_ddl := IF(
  @has_submission_assignment_idx = 0,
  'CREATE INDEX idx_assignment_id ON oj_submissions (assignment_id)',
  'SELECT 1'
);
PREPARE submission_assignment_idx_stmt FROM @submission_assignment_idx_ddl;
EXECUTE submission_assignment_idx_stmt;
DEALLOCATE PREPARE submission_assignment_idx_stmt;
