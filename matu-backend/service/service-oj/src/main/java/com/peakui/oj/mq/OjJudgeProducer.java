package com.peakui.oj.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OjJudgeProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendJudgeTask(Long submissionId) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.JUDGE_EXCHANGE, RabbitMQConfig.JUDGE_ROUTING_KEY, String.valueOf(submissionId));
    }
}
