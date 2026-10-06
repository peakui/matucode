package com.peakui.file.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_operations")
public class FileOperation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("file_id")
    private Long fileId;

    @TableField("user_id")
    private Long userId;

    @TableField("operation_type")
    private Integer operationType;

    @TableField("operation_ip")
    private String operationIp;

    @TableField("operation_info")
    private String operationInfo;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
