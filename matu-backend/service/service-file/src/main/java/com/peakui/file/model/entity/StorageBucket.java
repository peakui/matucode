package com.peakui.file.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("storage_buckets")
public class StorageBucket {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("bucket_type")
    private Integer bucketType;

    @TableField("storage_provider")
    private String storageProvider;

    private String endpoint;
    private String region;

    @TableField("max_size")
    private Long maxSize;

    @TableField("used_size")
    private Long usedSize;

    @TableField("file_count")
    private Integer fileCount;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
