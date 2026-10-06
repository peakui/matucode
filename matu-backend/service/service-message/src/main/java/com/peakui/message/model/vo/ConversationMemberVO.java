package com.peakui.message.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationMemberVO {
    private Long id;
    private Long conversationId;
    private Long userId;
    private Integer role;
    private Integer isMuted;
    private Integer isTop;
    private Long lastReadMessageId;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
}
