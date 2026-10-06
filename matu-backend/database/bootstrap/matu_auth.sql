-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `account_deletion_requests` (
  `user_id` bigint NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  `created_at` datetime NOT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `review_note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `certifications` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `cert_type` tinyint(1) NOT NULL COMMENT '认证类型(1学校/2企业/3头衔)',
  `cert_name` varchar(100) NOT NULL COMMENT '认证名称',
  `cert_proof` varchar(500) NOT NULL COMMENT '证明材料URL',
  `cert_status` tinyint(1) DEFAULT '0' COMMENT '状态(0待审核/1通过/2拒绝)',
  `audit_remark` varchar(200) DEFAULT NULL COMMENT '审核备注',
  `auditor_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_cert_status` (`cert_status`),
  KEY `idx_cert_type` (`cert_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证申请表';

CREATE TABLE `login_logs` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_type` tinyint(1) DEFAULT '1' COMMENT '登录方式(1密码/2短信/3第三方)',
  `login_ip` varchar(45) DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(100) DEFAULT NULL COMMENT '登录地点',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `login_status` tinyint(1) DEFAULT '1' COMMENT '状态(0失败/1成功)',
  `fail_reason` varchar(200) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_ip` (`login_ip`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表';

CREATE TABLE `mini_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `mini_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id` bigint NOT NULL COMMENT '角色ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码(USER/ADMIN/TEACHER/VIP)',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_desc` varchar(200) DEFAULT NULL COMMENT '角色描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0禁用/1启用)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE `user_external_identities` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `provider` varchar(32) NOT NULL,
  `app_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `open_id` varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `union_id` varchar(128) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_open` (`provider`,`app_id`,`open_id`),
  UNIQUE KEY `uk_provider_user` (`provider`,`app_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_follows` (
  `id` bigint NOT NULL COMMENT '主键',
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `following_id` bigint NOT NULL COMMENT '被关注ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower_id` (`follower_id`),
  KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系表';

CREATE TABLE `user_profiles` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID(外键)',
  `activity_level` bigint DEFAULT '0' COMMENT '活跃度',
  `school_name` varchar(100) DEFAULT NULL COMMENT '学校名称',
  `school_verified` tinyint(1) DEFAULT '0' COMMENT '学校认证(0未认证/1已认证)',
  `school_verify_time` datetime DEFAULT NULL COMMENT '学校认证时间',
  `company_name` varchar(100) DEFAULT NULL COMMENT '企业/公司名称',
  `company_verified` tinyint(1) DEFAULT '0' COMMENT '企业认证(0未认证/1已认证)',
  `company_verify_time` datetime DEFAULT NULL COMMENT '企业认证时间',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头衔/职位',
  `title_verified` tinyint(1) DEFAULT '0' COMMENT '头衔认证(0未认证/1已认证)',
  `major` varchar(50) DEFAULT NULL COMMENT '专业',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级/届别',
  `work_years` int DEFAULT '0' COMMENT '工作年限',
  `technical_stack` json DEFAULT NULL COMMENT '技术栈(数组)',
  `blog_url` varchar(255) DEFAULT NULL COMMENT '博客地址',
  `github_url` varchar(255) DEFAULT NULL COMMENT 'GitHub地址',
  `wechat_url` varchar(255) DEFAULT NULL COMMENT '微信二维码',
  `view_count` int DEFAULT '0' COMMENT '主页浏览次数',
  `follower_count` int DEFAULT '0' COMMENT '粉丝数',
  `following_count` int DEFAULT '0' COMMENT '关注数',
  `is_vip` tinyint(1) DEFAULT '0' COMMENT '是否VIP(0否/1是)',
  `vip_level` tinyint(1) DEFAULT '0' COMMENT 'VIP等级(0普通/1白银/2黄金/3钻石)',
  `vip_expired_at` datetime DEFAULT NULL COMMENT 'VIP过期时间',
  `vip_days_remaining` int DEFAULT '0' COMMENT '剩余VIP天数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_school_name` (`school_name`),
  KEY `idx_company_name` (`company_name`),
  KEY `idx_is_vip` (`is_vip`),
  KEY `idx_vip_expired_at` (`vip_expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户扩展信息表';

CREATE TABLE `user_roles` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';

CREATE TABLE `users` (
  `id` bigint NOT NULL COMMENT '用户ID(主键)',
  `username` varchar(50) NOT NULL COMMENT '用户名(唯一)',
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `email` varchar(100) NOT NULL COMMENT '邮箱(唯一)',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint(1) DEFAULT '0' COMMENT '性别(0女/1男/2保密)',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `signature` varchar(200) DEFAULT NULL COMMENT '个人签名',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0禁用/1正常/2封禁)',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间(软删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户主表';

CREATE TABLE `vip_activation_records` (
  `id` bigint NOT NULL,
  `activation_key` varchar(128) NOT NULL,
  `user_id` bigint NOT NULL,
  `days` int NOT NULL,
  `level` int NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vip_activation_key` (`activation_key`),
  KEY `idx_vip_activation_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `vip_orders` (
  `id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号(唯一)',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `vip_level` tinyint(1) NOT NULL COMMENT '购买等级(1/2/3)',
  `duration_days` int NOT NULL COMMENT '有效天数(30/90/365)',
  `amount` decimal(10,2) DEFAULT '0.00' COMMENT '支付金额',
  `pay_status` tinyint(1) DEFAULT '0' COMMENT '支付状态(0待支付/1已支付/2已取消)',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `pay_method` tinyint(1) DEFAULT '1' COMMENT '支付方式(1微信/2支付宝)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='VIP订单表';
