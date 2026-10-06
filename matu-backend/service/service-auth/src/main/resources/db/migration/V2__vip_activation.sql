-- V2: idempotent VIP grants issued by payment transactions.
CREATE TABLE IF NOT EXISTS vip_activation_records (
  id BIGINT NOT NULL PRIMARY KEY,
  activation_key VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL,
  days INT NOT NULL,
  level INT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_vip_activation_key (activation_key),
  KEY idx_vip_activation_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
