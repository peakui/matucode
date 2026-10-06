package com.peakui.post.mq;

import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeDatabaseStore;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

class LikeEventConsumerTest {
    private final LikeDatabaseStore database = mock(LikeDatabaseStore.class);
    private final LikeCacheService cache = mock(LikeCacheService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final Channel channel = mock(Channel.class);
    private final LikeEvent event = new LikeEvent("event", 42L, 7L, true, 1L, 1L);
    private final LikeEventConsumer consumer = new LikeEventConsumer(database, cache, redis);
    private Message message;
    private ValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(5L);
        message = new Message(new byte[0], properties);
    }

    @Test
    void successfulCommitClearsPendingAndRetryBeforeAck() throws Exception {
        consumer.consume(event, message, channel);
        var order = inOrder(database, cache, redis, channel);
        order.verify(database).apply(event);
        order.verify(cache).completed(event);
        order.verify(redis).delete("post:like:retry:event");
        order.verify(channel).basicAck(5L, false);
    }

    @Test
    void threeRetriesThenDeadLetter() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(database).apply(event);
        when(values.increment("post:like:retry:event")).thenReturn(1L, 2L, 3L, 4L);
        for (int i = 0; i < 4; i++) consumer.consume(event, message, channel);
        verify(channel, times(3)).basicNack(5L, false, true);
        verify(channel).basicNack(5L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void counterOutageCannotCauseInfiniteRequeue() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(database).apply(event);
        when(values.increment(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        consumer.consume(event, message, channel);
        verify(channel).basicNack(5L, false, false);
        verify(channel, never()).basicNack(5L, false, true);
    }

    @Test
    void invalidEventIsDeadLetteredRatherThanDiscarded() throws Exception {
        consumer.consume(null, message, channel);
        verify(channel).basicNack(5L, false, false);
        verifyNoInteractions(database, cache, redis);
    }

    @Test
    void cleanupFailureRetriesAlreadyIdempotentDatabaseWrite() throws Exception {
        doThrow(new IllegalStateException("redis unavailable")).when(cache).completed(event);
        when(values.increment(anyString())).thenReturn(1L);
        consumer.consume(event, message, channel);
        verify(database).apply(event);
        verify(channel).basicNack(5L, false, true);
    }
}
