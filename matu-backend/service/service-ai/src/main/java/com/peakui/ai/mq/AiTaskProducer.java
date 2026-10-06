package com.peakui.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.config.AiRabbitConfig;
import com.peakui.ai.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiTaskProducer {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(String taskId, String userId, ChatRequest request) {
        try {
            rabbitTemplate.convertAndSend(AiRabbitConfig.EXCHANGE, AiRabbitConfig.ROUTING_KEY,
                    objectMapper.writeValueAsString(Map.of("taskId", taskId, "userId", userId, "request", request)));
        } catch (Exception e) {
            throw new IllegalStateException("AI 异步任务提交失败", e);
        }
    }
}
