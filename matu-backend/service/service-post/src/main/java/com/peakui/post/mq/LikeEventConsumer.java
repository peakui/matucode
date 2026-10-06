package com.peakui.post.mq;

import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeDatabaseStore;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventConsumer {
    private static final String RETRY_PREFIX = "post:like:retry:";
    private static final int MAX_RETRIES = 3;

    private final LikeDatabaseStore database;
    private final LikeCacheService cache;
    private final StringRedisTemplate redis;

    @RabbitListener(queues = LikeRabbitMQConfig.QUEUE, containerFactory = "likeRabbitListenerContainerFactory")
    public void consume(LikeEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || !StringUtils.hasText(event.eventId())
                || event.postId() == null || event.userId() == null) {
            log.error("点赞事件无效，直接进入死信队列，eventId={}", event == null ? null : event.eventId());
            channel.basicNack(tag, false, false);
            return;
        }
        try {
            database.apply(event);
            cache.completed(event);
            redis.delete(RETRY_PREFIX + event.eventId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            handleFailure(event, tag, channel, e);
        }
    }

    private void handleFailure(LikeEvent event, long tag, Channel channel, Exception failure) throws IOException {
        String retryKey = RETRY_PREFIX + event.eventId();
        try {
            Long attempt = redis.opsForValue().increment(retryKey);
            redis.expire(retryKey, java.time.Duration.ofMinutes(10));
            if (attempt != null && attempt <= MAX_RETRIES) {
                log.warn("点赞事件处理失败，重新入队，eventId={}, attempt={}", event.eventId(), attempt, failure);
                channel.basicNack(tag, false, true);
                return;
            }
        } catch (Exception retryCounterFailure) {
            log.error("点赞事件重试计数失败，直接进入死信队列，eventId={}", event.eventId(), retryCounterFailure);
        }
        log.error("点赞事件超过最大重试次数，进入死信队列，eventId={}", event.eventId(), failure);
        channel.basicNack(tag, false, false);
    }
}
