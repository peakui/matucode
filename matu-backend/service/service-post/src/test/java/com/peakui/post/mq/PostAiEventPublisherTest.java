package com.peakui.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostAiEventPublisherTest {
    @Test
    void outboxEntryIsUpsertedTransactionallyForPost() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PostAiEventPublisher publisher = new PostAiEventPublisher(jdbc, new ObjectMapper());
        publisher.publish(new PostPublishedEvent(15L, "标题", "正文"));
        verify(jdbc).update(startsWith("INSERT INTO post_ai_outbox"), eq(15L), anyString(), anyString());
    }
}
