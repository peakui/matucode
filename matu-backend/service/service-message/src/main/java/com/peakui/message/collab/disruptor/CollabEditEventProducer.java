package com.peakui.message.collab.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.peakui.message.collab.model.dto.CollabEditPayload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class CollabEditEventProducer {

    private final RingBuffer<CollabEditEvent> ringBuffer;

    public CollabEditEventProducer(RingBuffer<CollabEditEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public boolean publish(Long userId, Long conversationId, Long documentId, String clientId, String requestId,
                           CollabEditPayload payload, WebSocketSession session) {
        long sequence;
        try {
            sequence = ringBuffer.tryNext();
        } catch (Exception e) {
            return false;
        }
        try {
            CollabEditEvent event = ringBuffer.get(sequence);
            event.setUserId(userId);
            event.setConversationId(conversationId);
            event.setDocumentId(documentId);
            event.setClientId(clientId);
            event.setRequestId(requestId);
            event.setPayload(payload);
            event.setSession(session);
        } finally {
            ringBuffer.publish(sequence);
        }
        return true;
    }
}
