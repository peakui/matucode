CREATE TABLE IF NOT EXISTS feedback_history (
    id BIGINT NOT NULL PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    from_status TINYINT NOT NULL,
    to_status TINYINT NOT NULL,
    priority TINYINT NOT NULL,
    assignee_id BIGINT NULL,
    reply_content MEDIUMTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_feedback_history (feedback_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
