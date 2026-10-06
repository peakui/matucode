-- V1: transactional outbox for post-published AI automation.
CREATE TABLE IF NOT EXISTS post_ai_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  payload JSON NOT NULL,
  created_at DATETIME NOT NULL,
  published_at DATETIME NULL,
  UNIQUE KEY uk_post_ai_outbox_post (post_id),
  KEY idx_post_ai_outbox_pending (published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
