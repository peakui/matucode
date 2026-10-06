-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `comments` (
  `id` bigint NOT NULL COMMENT '评论ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID(回复)',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `reply_count` int DEFAULT '0' COMMENT '回复数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0待审核/1显示/2隐藏/3删除)',
  `ip_address` varchar(45) DEFAULT NULL COMMENT '评论IP',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reply_to_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

CREATE TABLE `post_ai_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `event_id` varchar(64) NOT NULL,
  `payload` json NOT NULL,
  `created_at` datetime NOT NULL,
  `published_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_ai_outbox_post` (`post_id`),
  KEY `idx_post_ai_outbox_pending` (`published_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_categories` (
  `id` bigint NOT NULL COMMENT '分类ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `category_desc` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '图标URL',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `post_count` int DEFAULT '0' COMMENT '帖子数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表';

CREATE TABLE `post_collaborators` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '协作用户ID',
  `permission` tinyint(1) DEFAULT '1' COMMENT '权限(1只读/2编辑/3管理)',
  `invited_by` bigint NOT NULL COMMENT '邀请人ID',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子协作者表';

CREATE TABLE `post_collects` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `collect_folder_id` bigint DEFAULT NULL COMMENT '收藏夹ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子收藏表';

CREATE TABLE `post_contents` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `content_type` tinyint(1) DEFAULT '1' COMMENT '内容类型(1Markdown/2富文本/3代码)',
  `content` longtext NOT NULL COMMENT '正文内容',
  `word_count` int DEFAULT '0' COMMENT '字数统计',
  `read_time` int DEFAULT '0' COMMENT '预计阅读时间(分钟)',
  `version` int DEFAULT '1' COMMENT '版本号(协同编辑)',
  `last_edit_user_id` bigint DEFAULT NULL COMMENT '最后编辑用户ID',
  `last_edit_time` datetime DEFAULT NULL COMMENT '最后编辑时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子内容表';

CREATE TABLE `post_edit_history` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `version` int NOT NULL COMMENT '版本号',
  `editor_id` bigint NOT NULL COMMENT '编辑者ID',
  `editor_name` varchar(50) DEFAULT NULL COMMENT '编辑者名称',
  `content_snapshot` longtext COMMENT '内容快照',
  `change_desc` varchar(200) DEFAULT NULL COMMENT '修改说明',
  `change_type` tinyint(1) DEFAULT '1' COMMENT '变更类型(1创建/2编辑/3还原)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_version` (`version`),
  KEY `idx_editor_id` (`editor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子编辑历史表';

CREATE TABLE `post_images` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `image_type` varchar(20) DEFAULT NULL COMMENT '图片类型(封面/内容/附件)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子图片表';

CREATE TABLE `post_like_state` (
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `version` bigint NOT NULL,
  `liked` tinyint(1) NOT NULL,
  PRIMARY KEY (`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `post_likes` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子点赞表';

CREATE TABLE `post_reports` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint DEFAULT NULL COMMENT '帖子ID',
  `comment_id` bigint DEFAULT NULL COMMENT '评论ID',
  `reporter_id` bigint NOT NULL COMMENT '举报人ID',
  `report_type` tinyint(1) NOT NULL COMMENT '举报类型(1广告/2违规/3侵权/4其他)',
  `report_reason` varchar(500) NOT NULL COMMENT '举报原因',
  `report_status` tinyint(1) DEFAULT '0' COMMENT '处理状态(0待处理/1已处理/2已忽略)',
  `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `handle_result` varchar(500) DEFAULT NULL COMMENT '处理结果',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  PRIMARY KEY (`id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_report_status` (`report_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';

CREATE TABLE `post_tag_relations` (
  `id` bigint NOT NULL COMMENT '主键',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_tag` (`post_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子标签关联表';

CREATE TABLE `post_tags` (
  `id` bigint NOT NULL COMMENT '标签ID',
  `tag_name` varchar(50) NOT NULL COMMENT '标签名称',
  `tag_desc` varchar(200) DEFAULT NULL COMMENT '标签描述',
  `post_count` int DEFAULT '0' COMMENT '使用次数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0禁用/1启用)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`),
  KEY `idx_post_count` (`post_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';

CREATE TABLE `posts` (
  `id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `collect_count` int DEFAULT '0' COMMENT '收藏数',
  `share_count` int DEFAULT '0' COMMENT '分享数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0草稿/1发布/2审核/3删除)',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '置顶(0否/1是)',
  `is_essence` tinyint(1) DEFAULT '0' COMMENT '精华(0否/1是)',
  `is_lock` tinyint(1) DEFAULT '0' COMMENT '锁定(0否/1是)',
  `lock_reason` varchar(200) DEFAULT NULL COMMENT '锁定原因',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子主表';
