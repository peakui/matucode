-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `class_assignments` (
  `id` bigint NOT NULL COMMENT '主键',
  `class_id` bigint NOT NULL COMMENT '所属班级',
  `title` varchar(200) NOT NULL COMMENT '作业标题',
  `description` text COMMENT '作业要求说明',
  `type` tinyint NOT NULL COMMENT '类型 (1:OJ算法, 2:SQL闯关, 3:问答题/项目)',
  `problem_ids` json DEFAULT NULL COMMENT '关联题目ID列表',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `max_attempts` int DEFAULT '-1' COMMENT '最大提交次数 (-1为无限)',
  `is_public_rank` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开排行榜',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 (0:未开始, 1:进行中, 2:已结束, 3:已批改)',
  `created_by` bigint NOT NULL COMMENT '发布人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级作业/任务表';

CREATE TABLE `class_discussions` (
  `id` bigint NOT NULL COMMENT '主键',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `assignment_id` bigint DEFAULT NULL COMMENT '关联作业ID',
  `user_id` bigint NOT NULL COMMENT '发帖人',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text NOT NULL COMMENT '内容',
  `is_anonymous` tinyint NOT NULL DEFAULT '0' COMMENT '是否匿名',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (`id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_assignment_id` (`assignment_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级专属讨论区';

CREATE TABLE `class_members` (
  `id` bigint NOT NULL COMMENT '主键',
  `class_id` bigint NOT NULL COMMENT '班级ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` tinyint NOT NULL COMMENT '角色 (1:班主任, 2:助教, 3:学生)',
  `join_status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 (0:待审核, 1:正常, 2:已退出, 3:被移除)',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_user` (`class_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级成员表';

CREATE TABLE `class_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `assignment_id` bigint NOT NULL COMMENT '作业ID',
  `user_id` bigint NOT NULL COMMENT '提交学生ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `submission_id` bigint DEFAULT NULL COMMENT '关联全局判题系统的提交ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '作业状态 (0:未完成, 1:已完成/AC, 2:超时未交)',
  `score` decimal(5,2) DEFAULT '0.00' COMMENT '得分',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_user_problem` (`assignment_id`,`user_id`,`problem_id`),
  KEY `idx_submission_id` (`submission_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级作业提交记录表';

CREATE TABLE `classes` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '班级名称',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '班级类型 (1:普通班级, 2:竞赛)',
  `description` text COMMENT '班级简介/公告',
  `creator_id` bigint NOT NULL COMMENT '创建人ID (班主任)',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '班级封面图',
  `join_mode` tinyint NOT NULL DEFAULT '1' COMMENT '加入方式 (1:公开, 2:需审核, 3:仅邀请码)',
  `invite_code` varchar(20) DEFAULT NULL COMMENT '邀请码',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 (1:进行中, 2:已结束, 3:已解散)',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间 (竞赛用)',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间 (竞赛用)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级主表';

CREATE TABLE `oj_class_problem_relations` (
  `id` bigint NOT NULL COMMENT '主键',
  `class_id` bigint NOT NULL COMMENT '班级/竞赛ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `base_problem_id` bigint DEFAULT NULL COMMENT 'fork来源题目ID',
  `relation_type` tinyint NOT NULL DEFAULT '2' COMMENT '关联类型:1专属创建/2关联已有/3复制编辑',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0已移除/1有效',
  `created_by` bigint DEFAULT NULL COMMENT '操作人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_problem` (`class_id`,`problem_id`),
  KEY `idx_class_status` (`class_id`,`status`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_base_problem_id` (`base_problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级/竞赛题目关联表';

CREATE TABLE `oj_problem_contents` (
  `id` bigint NOT NULL COMMENT '主键',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `content_md` longtext COMMENT 'Markdown内容',
  `content_html` longtext COMMENT 'HTML内容',
  `starter_code_json` json DEFAULT NULL COMMENT '起始代码(多语言)',
  `solution_json` json DEFAULT NULL COMMENT '题解(多语言)',
  `tags_json` json DEFAULT NULL COMMENT '标签数组',
  `version` int DEFAULT '1' COMMENT '版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目内容扩展表';

CREATE TABLE `oj_problems` (
  `id` bigint NOT NULL COMMENT '题目ID',
  `problem_no` varchar(20) NOT NULL COMMENT '题目编号',
  `title` varchar(200) NOT NULL COMMENT '题目名称',
  `description` text NOT NULL COMMENT '题目描述',
  `input_format` text COMMENT '输入格式',
  `output_format` text COMMENT '输出格式',
  `sample_input` text COMMENT '样例输入',
  `sample_output` text COMMENT '样例输出',
  `hint` text COMMENT '提示',
  `difficulty` tinyint(1) DEFAULT '1' COMMENT '难度(1简单/2中等/3困难)',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID（默认1：是oj能看到的，2是班级/刷题）',
  `time_limit` int DEFAULT '1000' COMMENT '时间限制(ms)',
  `memory_limit` int DEFAULT '256' COMMENT '内存限制(MB)',
  `submit_count` int DEFAULT '0' COMMENT '提交数',
  `accept_count` int DEFAULT '0' COMMENT '通过数',
  `accept_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0隐藏/1发布/2下架)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_no` (`problem_no`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OJ题目表';

CREATE TABLE `oj_problemset_items` (
  `id` bigint NOT NULL COMMENT '主键',
  `problemset_id` bigint NOT NULL COMMENT '题单ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `problem_type` tinyint(1) DEFAULT '1' COMMENT '题目类型(1算法/2SQL)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problemset_problem` (`problemset_id`,`problem_id`),
  KEY `idx_problemset_id` (`problemset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题单题目关联表';

CREATE TABLE `oj_problemsets` (
  `id` bigint NOT NULL COMMENT '题单ID',
  `title` varchar(200) NOT NULL COMMENT '题单标题',
  `description` text COMMENT '题单描述',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面URL',
  `problem_count` int DEFAULT '0' COMMENT '题目数',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `is_public` tinyint(1) DEFAULT '1' COMMENT '是否公开',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题单表';

CREATE TABLE `oj_submission_details` (
  `id` bigint NOT NULL COMMENT '主键',
  `submission_id` bigint NOT NULL COMMENT '提交ID',
  `case_no` int NOT NULL COMMENT '用例编号',
  `status` tinyint(1) DEFAULT '0' COMMENT '用例状态',
  `execution_time` int DEFAULT '0' COMMENT '执行时间(ms)',
  `memory_used` int DEFAULT '0' COMMENT '内存使用(KB)',
  `output` text COMMENT '实际输出',
  `expected_output` text COMMENT '期望输出',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提交详情表';

CREATE TABLE `oj_submissions` (
  `id` bigint NOT NULL COMMENT '提交ID',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `language` varchar(20) NOT NULL COMMENT '编程语言',
  `code` longtext NOT NULL COMMENT '提交代码',
  `code_length` int DEFAULT '0' COMMENT '代码长度',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0待判题/1AC/2WA/3TLE/4MLE/5RE/6CE/7SE)',
  `execution_time` int DEFAULT '0' COMMENT '执行时间(ms)',
  `memory_used` int DEFAULT '0' COMMENT '内存使用(KB)',
  `pass_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率',
  `passed_cases` int DEFAULT '0' COMMENT '通过用例数',
  `total_cases` int DEFAULT '0' COMMENT '总用例数',
  `error_message` text COMMENT '错误信息',
  `judge_time` datetime DEFAULT NULL COMMENT '判题时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提交记录表';

CREATE TABLE `oj_test_cases` (
  `id` bigint NOT NULL COMMENT '主键',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `case_no` int NOT NULL COMMENT '用例编号',
  `input` longtext NOT NULL COMMENT '输入数据',
  `expected_output` longtext NOT NULL COMMENT '期望输出',
  `is_sample` tinyint(1) DEFAULT '0' COMMENT '是否样例(0否/1是)',
  `score_weight` decimal(5,2) DEFAULT '1.00' COMMENT '分值权重',
  `is_hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_is_sample` (`is_sample`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试用例表';

CREATE TABLE `oj_user_stats` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_submissions` int DEFAULT '0' COMMENT '总提交数',
  `total_accepted` int DEFAULT '0' COMMENT '总通过数',
  `easy_accepted` int DEFAULT '0' COMMENT '简单通过数',
  `medium_accepted` int DEFAULT '0' COMMENT '中等通过数',
  `hard_accepted` int DEFAULT '0' COMMENT '困难通过数',
  `sql_accepted` int DEFAULT '0' COMMENT 'SQL通过数',
  `ranking` int DEFAULT '0' COMMENT '排名',
  `score` int DEFAULT '0' COMMENT '积分',
  `last_submit_time` datetime DEFAULT NULL COMMENT '最后提交时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_score` (`score`),
  KEY `idx_ranking` (`ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户刷题统计';

CREATE TABLE `sql_problems` (
  `id` bigint NOT NULL COMMENT '题目ID',
  `problem_no` varchar(20) NOT NULL COMMENT '题目编号',
  `title` varchar(200) NOT NULL COMMENT '题目名称',
  `description` text NOT NULL COMMENT '题目描述',
  `difficulty` tinyint(1) DEFAULT '1' COMMENT '难度(1简单/2中等/3困难)',
  `category` varchar(50) DEFAULT NULL COMMENT '分类(SELECT/JOIN/GROUP BY等)',
  `schema_sql` text COMMENT '表结构SQL',
  `init_data_sql` text COMMENT '初始化数据SQL',
  `expected_sql` text COMMENT '参考答案SQL',
  `hint` text COMMENT '提示',
  `submit_count` int DEFAULT '0' COMMENT '提交数',
  `accept_count` int DEFAULT '0' COMMENT '通过数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_problem_no` (`problem_no`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SQL题目表';

CREATE TABLE `sql_submissions` (
  `id` bigint NOT NULL COMMENT '主键',
  `problem_id` bigint NOT NULL COMMENT '题目ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sql_code` text NOT NULL COMMENT '提交SQL',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态(0待判题/1AC/2WA/3ERROR)',
  `execution_time` int DEFAULT '0' COMMENT '执行时间(ms)',
  `result_data` json DEFAULT NULL COMMENT '执行结果',
  `expected_data` json DEFAULT NULL COMMENT '期望结果',
  `error_message` text COMMENT '错误信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_problem_id` (`problem_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SQL提交记录表';
