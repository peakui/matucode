package com.peakui.message.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知信息。
 */
@Data
@Builder
public class NotificationVO {

    private Long id;
    private String type;
    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Long commentId;
    private Long fromUserId;
    private String fromNickname;
    private String fromAvatar;
    private String contentPreview;
    private Integer isRead;
    private LocalDateTime createdAt;
}
