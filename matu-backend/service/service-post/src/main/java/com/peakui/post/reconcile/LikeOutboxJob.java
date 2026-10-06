package com.peakui.post.reconcile;

import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeKeys;
import com.peakui.post.mq.LikeEvent;
import com.peakui.post.mq.LikeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeOutboxJob {
    private final StringRedisTemplate redis;
    private final LikeCacheService cache;
    private final LikeEventPublisher publisher;
    @Value("${post.like.outbox.enabled:true}") private boolean enabled;
    @Value("${post.like.outbox.batch-size:200}") private int batchSize;

    @Scheduled(fixedDelayString = "${post.like.outbox.fixed-delay-ms:5000}")
    public void publishPending() {
        if (!enabled) return;
        ScanOptions options = ScanOptions.scanOptions().match("post:like:*:outbox").count(batchSize).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try (Cursor<Map.Entry<Object, Object>> entries = redis.opsForHash().scan(key,
                        ScanOptions.scanOptions().count(batchSize).build())) {
                    entries.forEachRemaining(entry -> {
                        try {
                            LikeEvent event = cache.decode(String.valueOf(entry.getValue()));
                            if (publisher.publish(event) == LikeEventPublisher.Outcome.CONFIRMED) cache.completed(event);
                        } catch (Exception e) {
                            log.warn("点赞待发送事件处理失败，key={}, field={}", key, entry.getKey(), e);
                        }
                    });
                }
            }
        }
    }
}
