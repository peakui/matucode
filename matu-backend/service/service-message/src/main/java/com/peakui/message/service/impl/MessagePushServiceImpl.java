package com.peakui.message.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.message.model.vo.MessagePushEventVO;
import com.peakui.message.service.MessagePushService;
import com.peakui.message.websocket.MessageWebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePushServiceImpl implements MessagePushService {

    private final ObjectMapper objectMapper;
    private final MessageWebSocketSessionManager sessionManager;

    @Override
    public void pushToUsers(Collection<Long> userIds, MessagePushEventVO event) {
        if (userIds == null || userIds.isEmpty() || event == null) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("serialize message push event failed, event={}", event, e);
            return;
        }
        new HashSet<>(userIds).stream()
                .filter(Objects::nonNull)
                .forEach(userId -> sessionManager.sendToUser(userId, payload));
    }
}
