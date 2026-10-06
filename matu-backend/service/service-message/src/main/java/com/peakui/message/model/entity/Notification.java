package com.peakui.message.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知（评论回复等）。
 */
@Data
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String type;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Long commentId;
    private Long parentCommentId;
    private Long fromUserId;
    private String fromNickname;
    private String fromAvatar;
    private String contentPreview;
    private Integer isRead;
    private LocalDateTime createdAt;
}
