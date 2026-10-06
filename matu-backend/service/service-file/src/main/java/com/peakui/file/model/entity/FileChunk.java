package com.peakui.file.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_chunks")
public class FileChunk {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("file_id")
    private Long fileId;

    @TableField("chunk_no")
    private Integer chunkNo;

    @TableField("chunk_size")
    private Integer chunkSize;

    @TableField("chunk_md5")
    private String chunkMd5;

    @TableField("chunk_path")
    private String chunkPath;

    @TableField("upload_status")
    private Integer uploadStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
