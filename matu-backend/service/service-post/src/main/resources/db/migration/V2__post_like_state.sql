-- V3: durable per-user like versions and uniqueness for the like pair.
CREATE TABLE IF NOT EXISTS post_like_state (
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  version BIGINT NOT NULL,
  liked BOOLEAN NOT NULL,
  PRIMARY KEY (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Keep historical duplicate rows visible: a duplicate pair makes this DDL fail.
-- An existing exact unique (post_id,user_id) index is equivalent and is reused.
SET @has_post_like_pair_unique := (
  SELECT COUNT(*)
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'post_likes'
      AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 2
       AND COUNT(DISTINCT column_name) = 2
       AND SUM(column_name = 'post_id' AND sub_part IS NULL) = 1
       AND SUM(column_name = 'user_id' AND sub_part IS NULL) = 1
  ) AS pair_indexes
);

SET @post_like_pair_ddl := IF(
  @has_post_like_pair_unique = 0,
  'ALTER TABLE post_likes ADD UNIQUE KEY uk_post_likes_post_user (post_id,user_id)',
  'SELECT 1'
);
PREPARE post_like_pair_stmt FROM @post_like_pair_ddl;
EXECUTE post_like_pair_stmt;
DEALLOCATE PREPARE post_like_pair_stmt;
