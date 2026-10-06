package com.peakui.post.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Synchronous listener: the outbox row commits or rolls back with the article. */
@Component
@RequiredArgsConstructor
public class PostAiEventListener {
    private final PostAiEventPublisher publisher;

    @EventListener
    public void onPostPublished(PostPublishedEvent event) {
        publisher.publish(event);
    }
}
