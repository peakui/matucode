-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `qa_answers` (
  `id` bigint NOT NULL COMMENT '回答ID',
  `question_id` bigint NOT NULL COMMENT '问题ID',
  `user_id` bigint NOT NULL COMMENT '回答者ID',
  `content` text NOT NULL COMMENT '回答内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `dislike_count` int DEFAULT '0' COMMENT '踩数',
  `is_accepted` tinyint DEFAULT '0' COMMENT '是否采纳(0否/1是)',
  `accepted_at` datetime DEFAULT NULL COMMENT '采纳时间',
  `status` tinyint DEFAULT '1' COMMENT '状态(0待审核/1显示/2隐藏/3删除)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_accepted` (`is_accepted`),
  KEY `idx_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='回答表';

CREATE TABLE `qa_categories` (
  `id` bigint NOT NULL COMMENT '分类ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `category_desc` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `question_count` int DEFAULT '0' COMMENT '问题数',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问答分类表';

CREATE TABLE `qa_comments` (
  `id` bigint NOT NULL COMMENT '主键',
  `target_type` tinyint NOT NULL COMMENT '目标类型(1问题/2回答)',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `content` varchar(1000) NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reply_to_user_id` bigint DEFAULT NULL,
  `reply_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问答评论表';

CREATE TABLE `qa_follows` (
  `id` bigint NOT NULL COMMENT '主键',
  `question_id` bigint NOT NULL COMMENT '问题ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_user` (`question_id`,`user_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问题关注表';

CREATE TABLE `qa_questions` (
  `id` bigint NOT NULL COMMENT '问题ID',
  `user_id` bigint NOT NULL COMMENT '提问者ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `title` varchar(200) NOT NULL COMMENT '问题标题',
  `content` text NOT NULL COMMENT '问题内容',
  `bounty_points` int DEFAULT '0' COMMENT '悬赏积分',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `answer_count` int DEFAULT '0' COMMENT '回答数',
  `follow_count` int DEFAULT '0' COMMENT '关注数',
  `status` tinyint DEFAULT '1' COMMENT '状态(0待解决/1已解决/2已关闭/3删除)',
  `best_answer_id` bigint DEFAULT NULL COMMENT '最佳答案ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `solved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `share_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_bounty_points` (`bounty_points`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='问题表';

CREATE TABLE `qa_votes` (
  `id` bigint NOT NULL COMMENT '主键',
  `target_type` tinyint NOT NULL COMMENT '目标类型(1问题/2回答)',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `vote_type` tinyint NOT NULL COMMENT '投票类型(1赞/2踩)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_user` (`target_type`,`target_id`,`user_id`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_user_target_vote` (`user_id`,`target_type`,`vote_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投票表';

CREATE TABLE `qa_word_clouds` (
  `id` bigint NOT NULL COMMENT '主键',
  `question_id` bigint NOT NULL COMMENT '问题ID',
  `word_data` json NOT NULL COMMENT '词云数据(词频JSON)',
  `image_url` varchar(500) DEFAULT NULL COMMENT '词云图片URL',
  `generated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='词云图表';
