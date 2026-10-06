package com.peakui.ai.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.agent.AiAgentService;
import com.peakui.ai.config.AiRabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskConsumer {
    private final ObjectMapper objectMapper;
    private final AiAgentService agentService;
    private final StringRedisTemplate redis;

    @RabbitListener(queues = AiRabbitConfig.QUEUE)
    public void consume(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode root = objectMapper.readTree(payload);
            String taskId = root.path("taskId").asText();
            String userId = root.path("userId").asText();
            com.peakui.ai.model.ChatRequest request = objectMapper.treeToValue(root.path("request"), com.peakui.ai.model.ChatRequest.class);
            redis.opsForHash().put("ai:task:" + taskId, "status", "RUNNING");
            redis.expire("ai:task:" + taskId, Duration.ofHours(1));
            StringBuilder answer = new StringBuilder();
            agentService.streamAsUser(userId, request).doOnNext(answer::append).blockLast();
            redis.opsForHash().put("ai:task:" + taskId, "status", "SUCCESS");
            redis.opsForHash().put("ai:task:" + taskId, "answer", answer.toString());
        } catch (Exception e) {
            log.error("AI async task failed", e);
            String taskId = extractTaskId(payload);
            if (taskId != null) {
                redis.opsForHash().put("ai:task:" + taskId, "status", "FAILED");
                redis.opsForHash().put("ai:task:" + taskId, "message", "任务执行失败");
            }
            throw new IllegalStateException(e);
        }
    }

    private String extractTaskId(String payload) {
        try { return objectMapper.readTree(payload).path("taskId").asText(null); }
        catch (Exception ignored) { return null; }
    }
}
