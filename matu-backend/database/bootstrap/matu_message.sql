-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `collab_documents` (
  `id` bigint NOT NULL COMMENT '协作文档ID',
  `conversation_id` bigint NOT NULL COMMENT '关联会话ID',
  `title` varchar(200) DEFAULT NULL COMMENT '文档标题',
  `content` mediumtext COMMENT '当前文档内容',
  `revision` bigint NOT NULL DEFAULT '0' COMMENT '当前版本号',
  `created_by` bigint NOT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_id` (`conversation_id`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='协作文档表';

CREATE TABLE `collab_operations` (
  `id` bigint NOT NULL COMMENT '操作ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `revision` bigint NOT NULL COMMENT '服务端提交后的版本号',
  `base_revision` bigint NOT NULL COMMENT '客户端提交时基于的版本号',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端实例ID',
  `request_id` varchar(64) NOT NULL COMMENT '客户端请求ID',
  `operation_type` tinyint NOT NULL COMMENT '操作类型:1插入/2删除',
  `position` int NOT NULL COMMENT '原始操作位置',
  `text` text COMMENT '插入文本',
  `length` int DEFAULT NULL COMMENT '删除长度',
  `transformed_position` int NOT NULL COMMENT 'OT转换后位置',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_request` (`client_id`,`request_id`),
  UNIQUE KEY `uk_document_revision` (`document_id`,`revision`),
  KEY `idx_document_base_revision` (`document_id`,`base_revision`),
  KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='协同编辑操作表';

CREATE TABLE `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `conversation_type` tinyint(1) DEFAULT '1' COMMENT '会话类型(1单聊/2群聊)',
  `conversation_name` varchar(100) DEFAULT NULL COMMENT '会话名称(群聊)',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后消息ID',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后消息时间',
  `member_count` int DEFAULT '0' COMMENT '成员数',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表';

CREATE TABLE `notifications` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL COMMENT '收件人',
  `type` varchar(32) NOT NULL COMMENT 'comment_reply',
  `source_type` varchar(16) NOT NULL COMMENT 'post | check | question',
  `source_id` bigint NOT NULL,
  `source_title` varchar(255) DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `parent_comment_id` bigint DEFAULT NULL,
  `from_user_id` bigint NOT NULL,
  `from_nickname` varchar(64) DEFAULT NULL,
  `from_avatar` varchar(512) DEFAULT NULL,
  `content_preview` varchar(500) DEFAULT NULL,
  `is_read` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_read_created` (`user_id`,`is_read`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `conversation_members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` tinyint(1) DEFAULT '1' COMMENT '角色(1成员/2管理员/3群主)',
  `is_muted` tinyint(1) DEFAULT '0' COMMENT '是否免打扰',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
  `last_read_message_id` bigint DEFAULT NULL COMMENT '最后已读消息ID',
  `unread_count` int DEFAULT '0' COMMENT '未读数',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `left_at` datetime DEFAULT NULL COMMENT '退出时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conv_user` (`conversation_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_unread_count` (`unread_count`),
  CONSTRAINT `fk_member_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话成员表';

CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `message_type` tinyint(1) DEFAULT '1' COMMENT '消息类型(1文本/2图片/3文件/4语音/5视频)',
  `content` text COMMENT '消息内容',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件URL',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小',
  `reply_to_id` bigint DEFAULT NULL COMMENT '回复消息ID',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `is_recall` tinyint(1) DEFAULT '0' COMMENT '是否撤回',
  `recall_time` datetime DEFAULT NULL COMMENT '撤回时间',
  `read_status` tinyint(1) DEFAULT '0' COMMENT '已读状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';

CREATE TABLE `message_reads` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_id` bigint NOT NULL COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `read_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_user` (`message_id`,`user_id`),
  KEY `idx_message_id` (`message_id`),
  CONSTRAINT `fk_read_message` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息已读表';
