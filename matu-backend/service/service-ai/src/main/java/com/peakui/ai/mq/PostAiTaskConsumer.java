package com.peakui.ai.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostAiTaskConsumer {
    private final ObjectMapper objectMapper;
    private final PostAiAutomationService automationService;

    @RabbitListener(queues = PostAiRabbitConfig.QUEUE, concurrency = "1", ackMode = "AUTO")
    public void consume(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode root = objectMapper.readTree(payload);
            Long postId = root.path("postId").asLong(0);
            if (postId <= 0) {
                throw new IllegalArgumentException("AI 帖子事件缺少 postId");
            }
            automationService.process(postId,
                    root.path("title").asText(""),
                    root.path("content").asText(""));
        } catch (Exception error) {
            log.error("帖子发布后的 AI 自动化任务失败", error);
            throw new IllegalStateException(error);
        }
    }
}
