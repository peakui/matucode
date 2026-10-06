-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `course_articles` (
  `id` bigint NOT NULL COMMENT '主键',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `content` longtext NOT NULL COMMENT '文章内容',
  `word_count` int DEFAULT '0' COMMENT '字数',
  `read_time` int DEFAULT '0' COMMENT '阅读时间(分钟)',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_chapter_id` (`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文字教程表';

CREATE TABLE `course_certificates` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `certificate_no` varchar(50) NOT NULL COMMENT '证书编号',
  `certificate_url` varchar(500) DEFAULT NULL COMMENT '证书URL',
  `issue_date` date DEFAULT NULL COMMENT '颁发日期',
  `expire_date` date DEFAULT NULL COMMENT '过期日期',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificate_no` (`certificate_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程证书表';

CREATE TABLE `course_chapters` (
  `id` bigint NOT NULL COMMENT '章节ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `chapter_title` varchar(200) NOT NULL COMMENT '章节标题',
  `chapter_desc` varchar(500) DEFAULT NULL COMMENT '章节描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `video_count` int DEFAULT '0' COMMENT '视频数',
  `duration` int DEFAULT '0' COMMENT '时长(秒)',
  `is_free_preview` tinyint(1) DEFAULT '0' COMMENT '是否免费试看',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程章节表';

CREATE TABLE `course_notes` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `video_id` bigint DEFAULT NULL COMMENT '视频ID',
  `content` text NOT NULL COMMENT '笔记内容',
  `timestamp` int DEFAULT '0' COMMENT '视频时间戳(秒)',
  `is_public` tinyint(1) DEFAULT '0' COMMENT '是否公开',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习笔记表';

CREATE TABLE `course_progress` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `video_id` bigint DEFAULT NULL COMMENT '视频ID',
  `progress_percent` decimal(5,2) DEFAULT '0.00' COMMENT '进度百分比',
  `watched_duration` int DEFAULT '0' COMMENT '已观看时长(秒)',
  `last_watch_time` datetime DEFAULT NULL COMMENT '最后观看时间',
  `is_completed` tinyint(1) DEFAULT '0' COMMENT '是否完成',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course_video` (`user_id`,`course_id`,`video_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习进度表';

CREATE TABLE `course_reviews` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `rating` tinyint(1) NOT NULL COMMENT '评分(1-5)',
  `content` text COMMENT '评价内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `is_verified_purchase` tinyint(1) DEFAULT '0' COMMENT '是否已购',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程评价表';

CREATE TABLE `course_videos` (
  `id` bigint NOT NULL COMMENT '视频ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `video_title` varchar(200) NOT NULL COMMENT '视频标题',
  `video_desc` varchar(500) DEFAULT NULL COMMENT '视频描述',
  `video_url` varchar(500) NOT NULL COMMENT '视频URL',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面URL',
  `duration` int DEFAULT '0' COMMENT '时长(秒)',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `resolution` varchar(20) DEFAULT NULL COMMENT '分辨率',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `play_count` int DEFAULT '0' COMMENT '播放次数',
  `is_free_preview` tinyint(1) DEFAULT '0' COMMENT '是否免费试看',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程视频表';

CREATE TABLE `courses` (
  `id` bigint NOT NULL COMMENT '课程ID',
  `instructor_id` bigint NOT NULL COMMENT '讲师ID',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `title` varchar(200) NOT NULL COMMENT '课程标题',
  `subtitle` varchar(300) DEFAULT NULL COMMENT '副标题',
  `description` text COMMENT '课程描述',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `price` decimal(10,2) DEFAULT '0.00' COMMENT '价格',
  `original_price` decimal(10,2) DEFAULT '0.00' COMMENT '原价',
  `level` tinyint(1) DEFAULT '1' COMMENT '难度(1入门/2进阶/3高级)',
  `language` varchar(20) DEFAULT 'zh-CN' COMMENT '语言',
  `student_count` int DEFAULT '0' COMMENT '学习人数',
  `chapter_count` int DEFAULT '0' COMMENT '章节数',
  `video_count` int DEFAULT '0' COMMENT '视频数',
  `total_duration` int DEFAULT '0' COMMENT '总时长(秒)',
  `rating` decimal(3,2) DEFAULT '0.00' COMMENT '评分',
  `rating_count` int DEFAULT '0' COMMENT '评分人数',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0草稿/1发布/2下架)',
  `is_free` tinyint(1) DEFAULT '0' COMMENT '是否免费',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_instructor_id` (`instructor_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_price` (`price`),
  KEY `idx_student_count` (`student_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程表';
