-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `announcements` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告内容(支持富文本/Markdown)',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '类型: 0-普通通知, 1-重要公告, 2-维护通知, 3-活动推广',
  `priority` int NOT NULL DEFAULT '0' COMMENT '排序权重(越大越靠前)',
  `is_pinned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶: 0-否, 1-是',
  `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间(NULL表示立即发布)',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间(NULL表示永久有效)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-草稿, 1-已发布, 2-已下架',
  `author_id` bigint unsigned DEFAULT NULL COMMENT '发布者用户ID',
  `click_count` int unsigned NOT NULL DEFAULT '0' COMMENT '阅读/点击次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_publish` (`status`,`publish_time`),
  KEY `idx_type_priority` (`type`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

CREATE TABLE `feedback_history` (
  `id` bigint NOT NULL,
  `feedback_id` bigint NOT NULL,
  `operator_id` bigint NOT NULL,
  `from_status` tinyint NOT NULL,
  `to_status` tinyint NOT NULL,
  `priority` tinyint NOT NULL,
  `assignee_id` bigint DEFAULT NULL,
  `reply_content` mediumtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_history` (`feedback_id`,`created_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `operation_logs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '操作用户ID(NULL表示系统/匿名)',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名(冗余存储防止用户删除后丢失)',
  `module` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '功能模块: user/post/comment/admin/system等',
  `action` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: create/update/delete/login/ban/report等',
  `target_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标资源类型: post/comment/user/file等',
  `target_id` bigint unsigned DEFAULT NULL COMMENT '目标资源ID',
  `detail` json DEFAULT NULL COMMENT '操作详情/变更快照(JSON格式)',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作IP(兼容IPv6)',
  `user_agent` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器UA信息',
  `result` tinyint NOT NULL DEFAULT '1' COMMENT '操作结果: 0-失败, 1-成功',
  `error_msg` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败时的错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module_action` (`module`,`action`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

CREATE TABLE `platform_metrics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `metric_date` date NOT NULL COMMENT '统计日期',
  `metric_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名: new_users/new_posts/active_users/api_calls等',
  `metric_value` bigint unsigned NOT NULL DEFAULT '0' COMMENT '指标值',
  `extra` json DEFAULT NULL COMMENT '扩展维度数据',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_key` (`metric_date`,`metric_key`),
  KEY `idx_metric_key` (`metric_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台运行指标统计表';

CREATE TABLE `sys_configs` (
  `config_key` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置说明',
  `group_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'general' COMMENT '配置分组: general/email/storage/security等',
  `is_public` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否前端可见: 0-仅后端, 1-前端可读取',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`),
  KEY `idx_group` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统全局配置表';

CREATE TABLE `user_feedbacks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '提交用户ID(NULL表示匿名/未登录反馈)',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名(冗余存储，防止用户注销后丢失)',
  `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系邮箱(便于离线回复)',
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '反馈类型: 0-Bug报告, 1-功能建议, 2-内容举报, 3-账号问题, 4-其他',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '反馈标题/摘要',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细描述(支持Markdown)',
  `attachments` json DEFAULT NULL COMMENT '附件列表(JSON数组): [{"name":"bug.png","url":"/uploads/xxx","size":1024}]',
  `extra_info` json DEFAULT NULL COMMENT '环境信息: {"browser":"Chrome120","os":"Win11","resolution":"1920x1080"}',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-待处理, 1-处理中, 2-已解决, 3-已拒绝, 4-已关闭',
  `priority` tinyint NOT NULL DEFAULT '1' COMMENT '优先级: 0-低, 1-中, 2-高, 3-紧急',
  `assignee_id` bigint unsigned DEFAULT NULL COMMENT '指派处理人(管理员ID)',
  `reply_content` text COLLATE utf8mb4_unicode_ci COMMENT '官方回复内容',
  `replied_at` datetime DEFAULT NULL COMMENT '首次回复时间',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决/关闭时间',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提交IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status_priority` (`status`,`priority`),
  KEY `idx_type_created` (`type`,`created_at`),
  KEY `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';
