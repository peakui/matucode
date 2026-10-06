package com.peakui.info.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_feedbacks")
public class UserFeedback {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String username;
    private String contactEmail;
    private Integer type;
    private String title;
    private String content;
    private String attachments;
    private String extraInfo;
    private Integer status;
    private Integer priority;
    private Long assigneeId;
    private String replyContent;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime repliedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime resolvedAt;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
