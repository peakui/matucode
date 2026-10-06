package com.peakui.message.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminConversationVO {

    private Long id;
    private Integer conversationType;
    private String conversationName;
    private Long creatorId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private String lastMessagePreview;
    private Integer memberCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ConversationMemberVO> members;
}
