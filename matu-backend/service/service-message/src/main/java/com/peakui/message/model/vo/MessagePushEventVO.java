package com.peakui.message.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessagePushEventVO {
    private String type;
    private Long conversationId;
    private Long messageId;
    private Long senderId;
    private List<Long> receiverUserIds;
    private Object data;
    private LocalDateTime timestamp;
}
