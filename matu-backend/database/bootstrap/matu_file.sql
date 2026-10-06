-- Schema only. Import into an EMPTY database, then start the service for Flyway migrations.
SET NAMES utf8mb4;

CREATE TABLE `file_chunks` (
  `id` bigint NOT NULL COMMENT '主键',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `chunk_no` int NOT NULL COMMENT '分片编号',
  `chunk_size` int DEFAULT '0' COMMENT '分片大小',
  `chunk_md5` varchar(32) DEFAULT NULL COMMENT '分片MD5',
  `chunk_path` varchar(500) DEFAULT NULL COMMENT '分片路径',
  `upload_status` tinyint(1) DEFAULT '0' COMMENT '上传状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_chunk` (`file_id`,`chunk_no`),
  KEY `idx_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件分片表';

CREATE TABLE `file_operations` (
  `id` bigint NOT NULL COMMENT '主键',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `operation_type` tinyint(1) NOT NULL COMMENT '操作类型(1上传/2下载/3删除/4分享)',
  `operation_ip` varchar(45) DEFAULT NULL COMMENT '操作IP',
  `operation_info` varchar(500) DEFAULT NULL COMMENT '操作详情',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件操作日志表';

CREATE TABLE `files` (
  `id` bigint NOT NULL COMMENT '文件ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_md5` varchar(32) DEFAULT NULL COMMENT '文件MD5',
  `bucket_name` varchar(50) DEFAULT NULL COMMENT '存储桶',
  `owner_id` bigint NOT NULL COMMENT '所有者ID',
  `owner_type` tinyint(1) DEFAULT '1' COMMENT '所有者类型(1用户/2系统)',
  `is_public` tinyint(1) DEFAULT '0' COMMENT '是否公开',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间',
  `upload_id` varchar(128) DEFAULT NULL COMMENT 'OSS分片上传ID',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_file_md5` (`file_md5`),
  KEY `idx_bucket_name` (`bucket_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件表';

CREATE TABLE `storage_buckets` (
  `id` bigint NOT NULL COMMENT '主键',
  `bucket_name` varchar(50) NOT NULL COMMENT '桶名称',
  `bucket_type` tinyint(1) DEFAULT '1' COMMENT '类型(1私有/2公开/3只读)',
  `storage_provider` varchar(20) DEFAULT 'minio' COMMENT '存储提供商',
  `endpoint` varchar(255) DEFAULT NULL COMMENT '访问端点',
  `region` varchar(50) DEFAULT NULL COMMENT '区域',
  `max_size` bigint DEFAULT '0' COMMENT '最大容量(字节)',
  `used_size` bigint DEFAULT '0' COMMENT '已用容量',
  `file_count` int DEFAULT '0' COMMENT '文件数',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bucket_name` (`bucket_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储桶表';
