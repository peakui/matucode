package com.peakui.message.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationVO {
    private Long id;
    private Integer conversationType;
    private String conversationName;
    private Long creatorId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer memberCount;
    private Integer isMuted;
    private Integer isTop;
    private Long lastReadMessageId;
    private Integer unreadCount;
    private MessageVO lastMessage;
    private List<ConversationMemberVO> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
