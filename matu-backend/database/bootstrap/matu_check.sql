-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `achievements` (
  `id` bigint NOT NULL COMMENT '成就ID',
  `achievement_code` varchar(50) NOT NULL COMMENT '成就编码',
  `achievement_name` varchar(50) NOT NULL COMMENT '成就名称',
  `achievement_desc` varchar(200) DEFAULT NULL COMMENT '成就描述',
  `achievement_icon` varchar(255) DEFAULT NULL COMMENT '成就图标',
  `achievement_type` tinyint(1) DEFAULT '1' COMMENT '类型(1打卡/2刷题/3学习/4文章/5互动)',
  `condition_type` tinyint(1) DEFAULT '1' COMMENT '条件类型(1天数/2次数/3连续/4浏览量/5点赞数)',
  `condition_value` int DEFAULT '0' COMMENT '条件值',
  `point_reward` int DEFAULT '0' COMMENT '积分奖励',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`achievement_code`),
  KEY `idx_type` (`achievement_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成就定义表';

CREATE TABLE `check_comments` (
  `id` bigint NOT NULL COMMENT '主键',
  `check_id` bigint NOT NULL COMMENT '关联的文章/打卡ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父评论ID',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '被回复人ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1正常/0隐藏)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reply_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_check_id` (`check_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章评论表';

CREATE TABLE `check_group_members` (
  `id` bigint NOT NULL COMMENT '主键',
  `group_id` bigint NOT NULL COMMENT '小组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` tinyint(1) DEFAULT '1' COMMENT '角色(1成员/2组长)',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1正常/2退出/3移除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`,`user_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打卡小组成员表';

CREATE TABLE `check_groups` (
  `id` bigint NOT NULL COMMENT '小组ID',
  `group_name` varchar(50) NOT NULL COMMENT '小组名称',
  `group_desc` varchar(200) DEFAULT NULL COMMENT '小组描述',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '小组封面图',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `member_count` int DEFAULT '0' COMMENT '成员数',
  `article_count` int DEFAULT '0' COMMENT '小组内文章总数',
  `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开(0私有/1公开)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打卡小组表';

CREATE TABLE `check_record_likes` (
  `id` bigint NOT NULL,
  `check_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_user` (`check_id`,`user_id`),
  KEY `idx_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `check_records` (
  `id` bigint NOT NULL COMMENT '主键/文章ID',
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `title` varchar(100) DEFAULT NULL COMMENT '文章标题',
  `summary` varchar(255) DEFAULT NULL COMMENT '文章摘要',
  `content` text COMMENT '文章正文/打卡内容',
  `image_urls` json DEFAULT NULL COMMENT '封面图或正文图片URL数组',
  `learn_hours` decimal(4,2) DEFAULT '0.00' COMMENT '关联学习时长(小时)',
  `mood` tinyint(1) DEFAULT '0' COMMENT '心情(1-5)',
  `location` varchar(100) DEFAULT NULL COMMENT '打卡地点',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '发布IP',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `like_count` int DEFAULT '0' COMMENT '点赞次数',
  `comment_count` int DEFAULT '0' COMMENT '评论次数',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶(0否/1是)',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精华(0否/1是)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(1已发布/2草稿/3删除/4仅自己可见)',
  `check_date` date NOT NULL COMMENT '打卡日期',
  `check_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '打卡/发布时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `share_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`check_date`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_view_count` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打卡文章记录表';

CREATE TABLE `check_statistics` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_days` int DEFAULT '0' COMMENT '累计打卡天数',
  `continuous_days` int DEFAULT '0' COMMENT '当前连续打卡天数',
  `max_continuous_days` int DEFAULT '0' COMMENT '历史最大连续天数',
  `total_articles` int DEFAULT '0' COMMENT '累计发布文章数',
  `total_likes_received` int DEFAULT '0' COMMENT '累计获得点赞数',
  `total_learn_hours` decimal(10,2) DEFAULT '0.00' COMMENT '总学习时长',
  `last_check_date` date DEFAULT NULL COMMENT '最后打卡日期',
  `check_year` int NOT NULL COMMENT '统计年份',
  `check_month` int NOT NULL COMMENT '统计月份',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ym` (`user_id`,`check_year`,`check_month`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户打卡与发文统计表';

CREATE TABLE `user_achievements` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `achievement_id` bigint NOT NULL COMMENT '成就ID',
  `achieved_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '获得时间',
  `progress` int DEFAULT '0' COMMENT '当前进度',
  `is_claimed` tinyint(1) DEFAULT '0' COMMENT '是否领取奖励',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ach` (`user_id`,`achievement_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户成就表';
