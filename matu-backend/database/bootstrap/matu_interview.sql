-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `interview_answers` (
  `id` bigint NOT NULL COMMENT '主键',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `answer_type` tinyint(1) DEFAULT '1' COMMENT '答案类型(1官方/2用户)',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID(用户答案)',
  `content` text NOT NULL COMMENT '答案内容',
  `content_type` tinyint(1) DEFAULT '1' COMMENT '内容类型(1文字/2代码/3混合)',
  `code_snippet` text COMMENT '代码片段',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `is_official` tinyint(1) DEFAULT '0' COMMENT '是否官方答案',
  `is_locked` tinyint(1) DEFAULT '0' COMMENT '是否锁定',
  `unlock_days` int DEFAULT '0' COMMENT '解锁所需打卡天数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_answer_type` (`answer_type`),
  KEY `idx_is_official` (`is_official`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试答案表';

CREATE TABLE `interview_categories` (
  `id` bigint NOT NULL COMMENT '分类ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `category_desc` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '图标',
  `question_count` int DEFAULT '0' COMMENT '题目数',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试分类表';

CREATE TABLE `interview_companies` (
  `id` bigint NOT NULL COMMENT '公司ID',
  `company_name` varchar(100) NOT NULL COMMENT '公司名称',
  `company_logo` varchar(255) DEFAULT NULL COMMENT '公司Logo',
  `company_type` varchar(50) DEFAULT NULL COMMENT '公司类型',
  `question_count` int DEFAULT '0' COMMENT '题目数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_company_name` (`company_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司表';

CREATE TABLE `interview_questions` (
  `id` bigint NOT NULL COMMENT '题目ID',
  `question_no` varchar(20) NOT NULL COMMENT '题目编号',
  `title` varchar(300) NOT NULL COMMENT '题目名称',
  `content` text NOT NULL COMMENT '题目内容',
  `answer` text COMMENT '参考答案',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `company_id` bigint DEFAULT NULL COMMENT '来源公司ID',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布用户ID',
  `position_tags` json DEFAULT NULL COMMENT '岗位标签',
  `difficulty` tinyint(1) DEFAULT '1' COMMENT '难度(1简单/2中等/3困难)',
  `frequency` int DEFAULT '0' COMMENT '出现频次',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `collect_count` int DEFAULT '0' COMMENT '收藏数',
  `is_locked` tinyint(1) DEFAULT '0' COMMENT '是否锁定(0否/1是)',
  `unlock_days` int DEFAULT '0' COMMENT '解锁所需打卡天数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_no` (`question_no`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_is_locked` (`is_locked`),
  KEY `idx_publisher_id` (`publisher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题目表';

CREATE TABLE `mock_interviews` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `interview_title` varchar(200) NOT NULL COMMENT '面试标题',
  `position` varchar(50) DEFAULT NULL COMMENT '面试岗位',
  `company` varchar(100) DEFAULT NULL COMMENT '面试公司',
  `question_ids` json DEFAULT NULL COMMENT '题目ID数组',
  `total_score` int DEFAULT '0' COMMENT '总分',
  `user_score` int DEFAULT '0' COMMENT '用户得分',
  `duration_minutes` int DEFAULT '0' COMMENT '时长(分钟)',
  `actual_duration` int DEFAULT '0' COMMENT '实际用时(分钟)',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0进行中/1已完成/2已过期)',
  `report_json` json DEFAULT NULL COMMENT '评估报告',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模拟面试表';

CREATE TABLE `user_question_progress` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `question_type` tinyint(1) DEFAULT '1' COMMENT '题目类型(1面试/2OJ/3SQL)',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0未做/1已做/2掌握/3需复习)',
  `answer_content` text COMMENT '用户答案',
  `last_practice_time` datetime DEFAULT NULL COMMENT '最后练习时间',
  `practice_count` int DEFAULT '0' COMMENT '练习次数',
  `mastery_level` tinyint(1) DEFAULT '0' COMMENT '掌握程度(1-5)',
  `next_review_time` datetime DEFAULT NULL COMMENT '下次复习时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question_type` (`user_id`,`question_id`,`question_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户题目进度表';

CREATE TABLE `user_wrong_questions` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `question_type` tinyint(1) DEFAULT '1' COMMENT '题目类型',
  `wrong_count` int DEFAULT '0' COMMENT '错误次数',
  `last_wrong_time` datetime DEFAULT NULL COMMENT '最后错误时间',
  `wrong_reason` varchar(500) DEFAULT NULL COMMENT '错误原因',
  `is_resolved` tinyint(1) DEFAULT '0' COMMENT '是否已解决',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question_type` (`user_id`,`question_id`,`question_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_resolved` (`is_resolved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本表';
