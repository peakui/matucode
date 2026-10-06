package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报表。
 */
@Data
@TableName("post_reports")
public class PostReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Long commentId;
    private Long reporterId;
    private Integer reportType;
    private String reportReason;
    private Integer reportStatus;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime handleTime;
    private LocalDateTime createdAt;
}
