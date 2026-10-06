package com.peakui.message.collab.websocket;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
@Builder
public class CollabSessionInfo {
    private String sessionId;
    private Long userId;
    private Long conversationId;
    private Long documentId;
    private String clientId;
    private Integer cursor;
    private Integer selectionStart;
    private Integer selectionEnd;
    private LocalDateTime joinedAt;
    private WebSocketSession session;
}
