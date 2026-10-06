package com.peakui.message.websocket;

import com.peakui.message.collab.model.dto.CollabPresencePayload;
import com.peakui.message.collab.model.vo.CollabOnlineUserVO;
import com.peakui.message.collab.websocket.CollabSessionInfo;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Slf4j
@Component
public class MessageWebSocketSessionManager {

    // sendMessage 不是线程安全的：Disruptor 线程、容器线程与推送线程会同时往同一
    // 个 session 写，裸写会抛 TEXT_PARTIAL_WRITING。装饰器负责串行化并缓冲发送。
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<String>> roomSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CollabSessionInfo> collabSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> concurrentSessions = new ConcurrentHashMap<>();

    public void addSession(Long userId, WebSocketSession session) {
        concurrentSessions.computeIfAbsent(session.getId(),
                id -> new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
        Set<WebSocketSession> userSessions = sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
        userSessions.add(session);
        log.info("add message websocket session, userId={}, sessionId={}, sessionCount={}", userId, session.getId(), userSessions.size());
    }

    public void removeSession(Long userId, WebSocketSession session) {
        concurrentSessions.remove(session.getId());
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(userId);
            }
        }
        leaveRoom(session);
    }

    public void joinRoom(Long userId, WebSocketSession session, Long conversationId, Long documentId, String clientId) {
        leaveRoom(session);
        CollabSessionInfo info = CollabSessionInfo.builder()
                .sessionId(session.getId())
                .userId(userId)
                .conversationId(conversationId)
                .documentId(documentId)
                .clientId(clientId)
                .joinedAt(LocalDateTime.now())
                .session(session)
                .build();
        collabSessions.put(session.getId(), info);
        roomSessions.computeIfAbsent(conversationId, key -> ConcurrentHashMap.newKeySet()).add(session.getId());
    }

    public CollabSessionInfo leaveRoom(WebSocketSession session) {
        CollabSessionInfo info = collabSessions.remove(session.getId());
        if (info == null) {
            return null;
        }
        Set<String> room = roomSessions.get(info.getConversationId());
        if (room != null) {
            room.remove(session.getId());
            if (room.isEmpty()) {
                roomSessions.remove(info.getConversationId());
            }
        }
        return info;
    }

    public CollabSessionInfo getSessionInfo(WebSocketSession session) {
        return collabSessions.get(session.getId());
    }

    public void updatePresence(WebSocketSession session, CollabPresencePayload payload) {
        CollabSessionInfo info = getSessionInfo(session);
        if (info == null || payload == null) {
            return;
        }
        info.setCursor(payload.getCursor());
        info.setSelectionStart(payload.getSelectionStart());
        info.setSelectionEnd(payload.getSelectionEnd());
    }

    public List<CollabOnlineUserVO> listRoomMembers(Long conversationId) {
        Set<String> sessionIds = roomSessions.get(conversationId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(collabSessions::get)
                .filter(info -> info != null && info.getSession().isOpen())
                .map(info -> CollabOnlineUserVO.builder()
                        .userId(info.getUserId())
                        .clientId(info.getClientId())
                        .cursor(info.getCursor())
                        .selectionStart(info.getSelectionStart())
                        .selectionEnd(info.getSelectionEnd())
                        .build())
                .toList();
    }

    public void broadcastToRoom(Long conversationId, String payload) {
        broadcastToRoomExcept(conversationId, null, payload);
    }

    public void broadcastToRoomExcept(Long conversationId, String excludedSessionId, String payload) {
        Set<String> sessionIds = roomSessions.get(conversationId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        for (String sessionId : sessionIds) {
            if (sessionId.equals(excludedSessionId)) {
                continue;
            }
            CollabSessionInfo info = collabSessions.get(sessionId);
            if (info == null) {
                continue;
            }
            sendToSession(info.getSession(), payload);
        }
    }

    public void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            log.info("no online websocket session for userId={}", userId);
            return;
        }
        log.info("send message websocket payload, userId={}, sessionCount={}", userId, userSessions.size());
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                removeSession(userId, session);
                continue;
            }
            sendToSession(session, payload);
        }
    }

    public void sendToSession(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        WebSocketSession target = concurrentSessions.getOrDefault(session.getId(), session);
        try {
            target.sendMessage(new TextMessage(payload));
        } catch (IOException | IllegalStateException e) {
            log.warn("send websocket message failed, sessionId={}", session.getId(), e);
            Object userId = session.getAttributes().get(MessageWebSocketHandler.USER_ID_ATTRIBUTE);
            if (userId instanceof Long id) {
                removeSession(id, session);
            }
        }
    }
}
