package com.peakui.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostAiOutboxJobTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final PostAiOutboxJob job = new PostAiOutboxJob(jdbc, rabbit, new ObjectMapper());

    private void pending() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of(
                "id", 7L, "event_id", "revision-a", "payload",
                "{\"postId\":42,\"title\":\"title\",\"content\":\"body\"}")));
    }

    private void confirm(boolean ack) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "rejected"));
            return null;
        }).when(rabbit).convertAndSend(eq(PostAiRabbitConfig.EXCHANGE), eq(PostAiRabbitConfig.ROUTING_KEY),
                any(PostPublishedEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void confirmedMessageOnlyMarksTheMatchingRevisionPublished() {
        pending();
        confirm(true);
        job.publishPending();
        verify(jdbc).update("UPDATE post_ai_outbox SET published_at=NOW() WHERE id=? AND event_id=? AND published_at IS NULL",
                7L, "revision-a");
    }

    @Test
    void negativeConfirmationKeepsMessagePending() {
        pending();
        confirm(false);
        job.publishPending();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void BrokerFailureKeepsMessagePending() {
        pending();
        doThrow(new IllegalStateException("broker unavailable")).when(rabbit).convertAndSend(
                eq(PostAiRabbitConfig.EXCHANGE), eq(PostAiRabbitConfig.ROUTING_KEY),
                any(PostPublishedEvent.class), any(MessagePostProcessor.class), any(CorrelationData.class));
        job.publishPending();
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
