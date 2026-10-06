package com.peakui.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationMemory {
    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public List<Message> load(String userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        String key = key(userId, conversationId);
        List<String> values = redis.opsForList().range(key, 0, MAX_MESSAGES - 1);
        List<Message> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                try {
                    result.add(objectMapper.readValue(value, Message.class));
                } catch (JsonProcessingException ignored) {
                    // 丢弃损坏的单条记忆，不影响当前会话。
                }
            }
        }
        return result;
    }

    public void append(String userId, String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        redis.opsForList().rightPush(key(userId, conversationId), node.toString());
        redis.opsForList().trim(key(userId, conversationId), -MAX_MESSAGES, -1);
        redis.expire(key(userId, conversationId), TTL);
    }

    private String key(String userId, String conversationId) {
        return "ai:memory:" + userId + ":" + conversationId;
    }

    public record Message(String role, String content) {
    }
}
