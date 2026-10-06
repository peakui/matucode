package com.peakui.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostAiOutboxJob {
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${post.ai.outbox-fixed-delay-ms:5000}")
    public void publishPending() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, event_id, payload FROM post_ai_outbox WHERE published_at IS NULL ORDER BY id LIMIT 50");
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            try {
                PostPublishedEvent event = objectMapper.readValue(String.valueOf(row.get("payload")), PostPublishedEvent.class);
                var correlation = new org.springframework.amqp.rabbit.connection.CorrelationData(
                        java.util.UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend(PostAiRabbitConfig.EXCHANGE, PostAiRabbitConfig.ROUTING_KEY, event,
                        message -> {
                            message.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                            return message;
                        }, correlation);
                var confirm = correlation.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ未确认文章总结任务或任务不可路由");
                }
                jdbc.update("UPDATE post_ai_outbox SET published_at=NOW() WHERE id=? AND event_id=? AND published_at IS NULL",
                        id, row.get("event_id"));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                log.error("发送帖子 AI 任务失败, outboxId={}", id, error);
            }
        }
    }
}
