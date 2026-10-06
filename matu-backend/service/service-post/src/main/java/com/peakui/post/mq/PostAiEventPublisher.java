package com.peakui.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Stored inside the article transaction; RabbitMQ downtime cannot lose publication events. */
@Component
@RequiredArgsConstructor
public class PostAiEventPublisher {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public void publish(PostPublishedEvent event) {
        try {
            jdbc.update("INSERT INTO post_ai_outbox (post_id, event_id, payload, created_at) VALUES (?, ?, ?, NOW()) "
                            + "ON DUPLICATE KEY UPDATE event_id=VALUES(event_id), payload=VALUES(payload), created_at=NOW(), published_at=NULL",
                    event.postId(), java.util.UUID.randomUUID().toString(), objectMapper.writeValueAsString(event));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("无法记录文章总结任务", e);
        }
    }
}
