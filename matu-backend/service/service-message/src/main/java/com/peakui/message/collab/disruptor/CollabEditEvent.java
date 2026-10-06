package com.peakui.message.collab.disruptor;

import com.peakui.message.collab.model.dto.CollabEditPayload;
import org.springframework.web.socket.WebSocketSession;

public class CollabEditEvent {
    private Long userId;
    private Long conversationId;
    private Long documentId;
    private String clientId;
    private String requestId;
    private CollabEditPayload payload;
    private WebSocketSession session;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public CollabEditPayload getPayload() {
        return payload;
    }

    public void setPayload(CollabEditPayload payload) {
        this.payload = payload;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public void clear() {
        userId = null;
        conversationId = null;
        documentId = null;
        clientId = null;
        requestId = null;
        payload = null;
        session = null;
    }
}
