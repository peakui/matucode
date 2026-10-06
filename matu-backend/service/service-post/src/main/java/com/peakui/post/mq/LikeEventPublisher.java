package com.peakui.post.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventPublisher {
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    public enum Outcome { CONFIRMED, REJECTED, UNKNOWN }

    private final RabbitTemplate rabbitTemplate;

    /** Blocking variant for background jobs: waits for the broker confirm before returning. */
    public Outcome publish(LikeEvent event) {
        CorrelationData correlation = new CorrelationData(event.eventId());
        try {
            send(event, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (correlation.getReturned() != null || (confirm != null && !confirm.isAck())) return Outcome.REJECTED;
            return confirm != null && confirm.isAck() ? Outcome.CONFIRMED : Outcome.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.UNKNOWN;
        } catch (Exception e) {
            // A connection/confirm timeout may occur AFTER the broker persisted the event.
            return Outcome.UNKNOWN;
        }
    }

    /**
     * Non-blocking variant for the request path. The send returns immediately and the broker
     * confirm is observed on the AMQP callback thread, so a slow or absent confirm can no longer
     * stall the HTTP response. On rejection {@code onRejected} runs off the request thread.
     */
    public void publish(LikeEvent event, Runnable onRejected) {
        CorrelationData correlation = new CorrelationData(event.eventId());
        correlation.getFuture().whenComplete((confirm, failure) -> {
            if (correlation.getReturned() != null || (confirm != null && !confirm.isAck())) {
                log.warn("点赞事件被 RabbitMQ 拒绝, eventId={}, cause={}", event.eventId(),
                        confirm != null ? confirm.getReason() : "returned");
                if (onRejected != null) {
                    onRejected.run();
                }
            }
        });
        try {
            send(event, correlation);
        } catch (Exception e) {
            // The event is still recorded in the outbox; the reconciliation job republishes it.
            log.warn("点赞事件发送失败，等待补偿任务重试, eventId={}", event.eventId(), e);
        }
    }

    private void send(LikeEvent event, CorrelationData correlation) {
        rabbitTemplate.convertAndSend(LikeRabbitMQConfig.EXCHANGE, LikeRabbitMQConfig.ROUTING_KEY, event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }, correlation);
    }
}
