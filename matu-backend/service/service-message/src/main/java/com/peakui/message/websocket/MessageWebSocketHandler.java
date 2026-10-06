package com.peakui.message.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.message.collab.disruptor.CollabEditEventProducer;
import com.peakui.message.collab.model.dto.CollabEditPayload;
import com.peakui.message.collab.model.dto.CollabPresencePayload;
import com.peakui.message.collab.model.dto.CollabWsInboundMessage;
import com.peakui.message.collab.model.vo.CollabRoomStateVO;
import com.peakui.message.collab.model.vo.CollabWsOutboundMessage;
import com.peakui.message.collab.service.CollabDocumentService;
import com.peakui.message.collab.websocket.CollabSessionInfo;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    public static final String USER_ID_ATTRIBUTE = "userId";
    private final MessageWebSocketSessionManager sessionManager;
    private final CollabDocumentService collabDocumentService;
    private final CollabEditEventProducer collabEditEventProducer;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (!(userId instanceof Long id)) {
            log.warn("message websocket missing authenticated user, sessionId={}, uri={}", session.getId(), session.getUri());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("未登录或登录已失效"));
            return;
        }
        sessionManager.addSession(id, session);
        log.info("message websocket connected, userId={}, sessionId={}, uri={}", id, session.getId(), session.getUri());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            sessionManager.sendToSession(session, "pong");
            return;
        }
        Long userId = currentUserId(session);
        try {
            CollabWsInboundMessage inbound = objectMapper.readValue(payload, CollabWsInboundMessage.class);
            handleCollabMessage(userId, session, inbound);
        } catch (Exception e) {
            log.warn("handle websocket message failed, sessionId={}", session.getId(), e);
            sendError(session, null, null, null, userId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        CollabSessionInfo info = sessionManager.getSessionInfo(session);
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            sessionManager.removeSession(id, session);
            if (info != null) {
                broadcastUserLeft(info);
            }
            log.info("message websocket disconnected, userId={}, sessionId={}", id, session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        CollabSessionInfo info = sessionManager.getSessionInfo(session);
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            sessionManager.removeSession(id, session);
            if (info != null) {
                broadcastUserLeft(info);
            }
        }
        log.warn("message websocket transport error, sessionId={}", session.getId(), exception);
    }

    private void handleCollabMessage(Long userId, WebSocketSession session, CollabWsInboundMessage inbound) throws JsonProcessingException {
        if (inbound.getType() == null) {
            throw new IllegalArgumentException("消息类型不能为空");
        }
        switch (inbound.getType()) {
            case "COLLAB_JOIN" -> handleJoin(userId, session, inbound);
            case "COLLAB_LEAVE" -> handleLeave(session, inbound);
            case "COLLAB_EDIT" -> handleEdit(userId, session, inbound);
            case "COLLAB_CURSOR", "COLLAB_TYPING" -> handlePresence(userId, session, inbound);
            default -> throw new IllegalArgumentException("不支持的消息类型");
        }
    }

    private void handleJoin(Long userId, WebSocketSession session, CollabWsInboundMessage inbound) {
        collabDocumentService.requireConversationMember(inbound.getConversationId(), userId);
        CollabRoomStateVO roomState = collabDocumentService.getRoomState(inbound.getDocumentId(), userId);
        if (!roomState.getDocument().getConversationId().equals(inbound.getConversationId())) {
            throw new IllegalArgumentException("文档不属于当前会话");
        }
        sessionManager.joinRoom(userId, session, inbound.getConversationId(), inbound.getDocumentId(), inbound.getClientId());
        sessionManager.sendToSession(session, toJson(CollabWsOutboundMessage.builder()
                .type("COLLAB_JOINED")
                .requestId(inbound.getRequestId())
                .conversationId(inbound.getConversationId())
                .documentId(inbound.getDocumentId())
                .senderId(userId)
                .revision(roomState.getDocument().getRevision())
                .payload(roomState)
                .timestamp(LocalDateTime.now())
                .build()));
        sessionManager.broadcastToRoomExcept(inbound.getConversationId(), session.getId(), toJson(CollabWsOutboundMessage.builder()
                .type("COLLAB_USER_JOINED")
                .requestId(inbound.getRequestId())
                .conversationId(inbound.getConversationId())
                .documentId(inbound.getDocumentId())
                .senderId(userId)
                .payload(sessionManager.listRoomMembers(inbound.getConversationId()))
                .timestamp(LocalDateTime.now())
                .build()));
    }

    private void handleLeave(WebSocketSession session, CollabWsInboundMessage inbound) {
        CollabSessionInfo info = sessionManager.leaveRoom(session);
        if (info != null) {
            broadcastUserLeft(info);
            sessionManager.sendToSession(session, toJson(CollabWsOutboundMessage.builder()
                    .type("COLLAB_LEFT")
                    .requestId(inbound.getRequestId())
                    .conversationId(info.getConversationId())
                    .documentId(info.getDocumentId())
                    .senderId(info.getUserId())
                    .timestamp(LocalDateTime.now())
                    .build()));
        }
    }

    private void handleEdit(Long userId, WebSocketSession session, CollabWsInboundMessage inbound) throws JsonProcessingException {
        CollabSessionInfo info = requireJoinedSession(session, inbound);
        CollabEditPayload editPayload = objectMapper.treeToValue(inbound.getPayload(), CollabEditPayload.class);
        boolean published = collabEditEventProducer.publish(userId, info.getConversationId(), info.getDocumentId(),
                info.getClientId(), inbound.getRequestId(), editPayload, session);
        if (!published) {
            sendError(session, inbound.getRequestId(), info.getConversationId(), info.getDocumentId(), userId, "SERVER_BUSY");
        }
    }

    private void handlePresence(Long userId, WebSocketSession session, CollabWsInboundMessage inbound) throws JsonProcessingException {
        CollabSessionInfo info = requireJoinedSession(session, inbound);
        CollabPresencePayload presencePayload = inbound.getPayload() == null ? new CollabPresencePayload()
                : objectMapper.treeToValue(inbound.getPayload(), CollabPresencePayload.class);
        sessionManager.updatePresence(session, presencePayload);
        sessionManager.broadcastToRoomExcept(info.getConversationId(), session.getId(), toJson(CollabWsOutboundMessage.builder()
                .type(inbound.getType() + "_BROADCAST")
                .requestId(inbound.getRequestId())
                .conversationId(info.getConversationId())
                .documentId(info.getDocumentId())
                .senderId(userId)
                .payload(presencePayload)
                .timestamp(LocalDateTime.now())
                .build()));
    }

    private CollabSessionInfo requireJoinedSession(WebSocketSession session, CollabWsInboundMessage inbound) {
        CollabSessionInfo info = sessionManager.getSessionInfo(session);
        if (info == null) {
            throw new IllegalArgumentException("请先加入协作房间");
        }
        if (!info.getConversationId().equals(inbound.getConversationId()) || !info.getDocumentId().equals(inbound.getDocumentId())) {
            throw new IllegalArgumentException("当前连接未加入该协作房间");
        }
        return info;
    }

    private void broadcastUserLeft(CollabSessionInfo info) {
        sessionManager.broadcastToRoom(info.getConversationId(), toJson(CollabWsOutboundMessage.builder()
                .type("COLLAB_USER_LEFT")
                .conversationId(info.getConversationId())
                .documentId(info.getDocumentId())
                .senderId(info.getUserId())
                .payload(sessionManager.listRoomMembers(info.getConversationId()))
                .timestamp(LocalDateTime.now())
                .build()));
    }

    private Long currentUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            return id;
        }
        throw new IllegalArgumentException("未登录或登录已失效");
    }

    private void sendError(WebSocketSession session, String requestId, Long conversationId, Long documentId, Long userId, String message) {
        sessionManager.sendToSession(session, toJson(CollabWsOutboundMessage.builder()
                .type("COLLAB_ERROR")
                .requestId(requestId)
                .conversationId(conversationId)
                .documentId(documentId)
                .senderId(userId)
                .payload(message)
                .timestamp(LocalDateTime.now())
                .build()));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("websocket消息序列化失败", e);
        }
    }
}
