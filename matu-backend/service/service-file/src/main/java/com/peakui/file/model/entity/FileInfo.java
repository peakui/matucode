package com.peakui.file.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("files")
public class FileInfo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("original_name")
    private String originalName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_type")
    private Integer ownerType;

    @TableField("is_public")
    private Integer isPublic;

    @TableField("download_count")
    private Integer downloadCount;

    private Integer status;

    @TableField("upload_id")
    private String uploadId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
