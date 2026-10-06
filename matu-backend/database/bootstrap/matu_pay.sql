-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `alipay_notify_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `notify_id` varchar(128) NOT NULL COMMENT '支付宝通知ID(用于去重)',
  `trade_no` varchar(64) DEFAULT NULL COMMENT '支付宝交易号',
  `out_trade_no` varchar(32) DEFAULT NULL COMMENT '商户订单号',
  `raw_body` text NOT NULL COMMENT '完整POST原始报文',
  `sign_verified` tinyint NOT NULL DEFAULT '0' COMMENT '0:验签失败 1:验签成功',
  `processed` tinyint NOT NULL DEFAULT '0' COMMENT '0:未处理 1:已处理',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_id` (`notify_id`),
  KEY `idx_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付宝异步通知日志';

CREATE TABLE `biz_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '业务订单号(全局唯一)',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `product_name` varchar(255) NOT NULL COMMENT '商品名称',
  `amount` decimal(10,2) NOT NULL COMMENT '订单金额(元)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:待支付 1:已支付 2:已关闭 3:已退款 4:部分退款',
  `pay_deadline` datetime DEFAULT NULL COMMENT '支付超时时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_created` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务订单表';

CREATE TABLE `pay_refund` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(32) NOT NULL COMMENT '退款单号',
  `transaction_no` varchar(32) NOT NULL COMMENT '原支付流水号',
  `order_no` varchar(32) NOT NULL COMMENT '业务订单号',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:退款中 1:退款成功 2:退款失败',
  `channel_refund_no` varchar(64) DEFAULT NULL COMMENT '支付宝退款单号',
  `refund_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_transaction_no` (`transaction_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';

CREATE TABLE `pay_transaction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `transaction_no` varchar(32) NOT NULL COMMENT '支付流水号(内部生成)',
  `order_no` varchar(32) NOT NULL COMMENT '关联业务订单号',
  `channel` varchar(20) NOT NULL DEFAULT 'ALIPAY' COMMENT '支付渠道: ALIPAY/WECHAT',
  `channel_trade_no` varchar(64) DEFAULT NULL COMMENT '支付宝交易号(trade_no)',
  `amount` decimal(10,2) NOT NULL COMMENT '实际支付金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:待支付 1:支付成功 2:支付失败 3:已关闭 4:处理中',
  `pay_time` datetime DEFAULT NULL COMMENT '渠道实际支付完成时间',
  `notify_content` text COMMENT '支付宝异步通知原始内容(用于对账/排查)',
  `extra_info` json DEFAULT NULL COMMENT '扩展字段(如buyer_id, fund_bill_list等)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  UNIQUE KEY `uk_channel_trade_no` (`channel_trade_no`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';
