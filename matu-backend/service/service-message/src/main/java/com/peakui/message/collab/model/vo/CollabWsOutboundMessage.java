package com.peakui.message.collab.model.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabWsOutboundMessage {
    private String type;
    private String requestId;
    private Long conversationId;
    private Long documentId;
    private Long senderId;
    private Long revision;
    private Object payload;
    private LocalDateTime timestamp;
}
