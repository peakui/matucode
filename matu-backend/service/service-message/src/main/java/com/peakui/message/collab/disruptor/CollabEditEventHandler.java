package com.peakui.message.collab.disruptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.peakui.message.collab.model.vo.CollabOperationVO;
import com.peakui.message.collab.model.vo.CollabWsOutboundMessage;
import com.peakui.message.collab.service.CollabOperationService;
import com.peakui.message.websocket.MessageWebSocketSessionManager;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollabEditEventHandler implements EventHandler<CollabEditEvent> {

    private final CollabOperationService collabOperationService;
    private final MessageWebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onEvent(CollabEditEvent event, long sequence, boolean endOfBatch) {
        try {
            CollabOperationVO operation = collabOperationService.applyOperation(event.getUserId(), event.getConversationId(),
                    event.getDocumentId(), event.getClientId(), event.getRequestId(), event.getPayload());
            String accepted = toJson(CollabWsOutboundMessage.builder()
                    .type("COLLAB_OPERATION_ACCEPTED")
                    .requestId(event.getRequestId())
                    .conversationId(event.getConversationId())
                    .documentId(event.getDocumentId())
                    .senderId(event.getUserId())
                    .revision(operation.getRevision())
                    .payload(operation)
                    .timestamp(LocalDateTime.now())
                    .build());
            sessionManager.sendToSession(event.getSession(), accepted);
            String broadcast = toJson(CollabWsOutboundMessage.builder()
                    .type("COLLAB_OPERATION_BROADCAST")
                    .requestId(event.getRequestId())
                    .conversationId(event.getConversationId())
                    .documentId(event.getDocumentId())
                    .senderId(event.getUserId())
                    .revision(operation.getRevision())
                    .payload(operation)
                    .timestamp(LocalDateTime.now())
                    .build());
            sessionManager.broadcastToRoomExcept(event.getConversationId(), event.getSession().getId(), broadcast);
        } catch (Exception e) {
            log.warn("handle collab edit event failed, userId={}, conversationId={}, documentId={}", event.getUserId(),
                    event.getConversationId(), event.getDocumentId(), e);
            sessionManager.sendToSession(event.getSession(), error(event, e.getMessage()));
        } finally {
            event.clear();
        }
    }

    private String error(CollabEditEvent event, String message) {
        return toJson(CollabWsOutboundMessage.builder()
                .type("COLLAB_ERROR")
                .requestId(event.getRequestId())
                .conversationId(event.getConversationId())
                .documentId(event.getDocumentId())
                .senderId(event.getUserId())
                .payload(message)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("websocket消息序列化失败", e);
        }
    }
}
