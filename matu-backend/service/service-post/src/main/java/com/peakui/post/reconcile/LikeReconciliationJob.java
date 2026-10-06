package com.peakui.post.reconcile;

import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeDatabaseStore;
import com.peakui.post.like.LikeKeys;
import com.peakui.post.like.LikeMutationResult;
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

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeReconciliationJob {
    private final StringRedisTemplate redis;
    private final LikeCacheService cache;
    private final LikeDatabaseStore database;
    private final LikeEventPublisher publisher;
    @Value("${post.like.reconciliation.enabled:true}") private boolean enabled;
    @Value("${post.like.reconciliation.batch-size:200}") private int batchSize;

    @Scheduled(cron = "${post.like.reconciliation.cron:0 0 2 * * ?}")
    public void reconcile() {
        if (!enabled) return;
        int repaired = 0;
        ScanOptions options = ScanOptions.scanOptions().match("post:like:*:meta").count(Math.max(1, batchSize)).build();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                String meta = cursor.next();
                Long postId = parsePostId(meta);
                if (postId == null) continue;
                try {
                    if ("1".equals(redis.<String, String>opsForHash().get(meta, "ready"))) {
                        repaired += reconcilePost(postId);
                    }
                } catch (Exception e) {
                    log.warn("帖子点赞对账失败，postId={}", postId, e);
                }
            }
        } catch (Exception e) {
            log.error("点赞对账扫描失败，repaired={}", repaired, e);
        }
        log.info("点赞对账完成，repaired={}", repaired);
    }

    // Stream Redis members and page MySQL users; never retain both full sets in memory.
    int reconcilePost(long postId) {
        int repaired = 0;
        try (Cursor<String> cursor = redis.opsForSet().scan(LikeKeys.users(postId),
                ScanOptions.scanOptions().count(Math.max(1, batchSize)).build())) {
            while (cursor.hasNext()) repaired += reconcileUser(postId, Long.parseLong(cursor.next()));
        }
        long after = 0;
        while (true) {
            List<Long> page = database.users(postId, after, Math.max(1, batchSize));
            if (page.isEmpty()) break;
            for (Long userId : page) repaired += reconcileUser(postId, userId);
            after = page.get(page.size() - 1);
        }
        database.repairCount(postId);
        return repaired;
    }

    int reconcileUser(long postId, long userId) {
        // Redis is the accepted-write source of truth. A stale scan is only a candidate list.
        LikeCacheService.Snapshot snapshot = cache.snapshot(postId, userId);
        if (snapshot.liked() == database.liked(postId, userId)) return 0;
        LikeMutationResult mutation = cache.reconcileMutation(postId, userId, snapshot);
        if (mutation == null) return 0; // A concurrent request won; reconcile again next run.
        LikeEvent event = new LikeEvent(mutation.eventId(), postId, userId, snapshot.liked(),
                mutation.version(), System.currentTimeMillis());
        if (publisher.publish(event) == LikeEventPublisher.Outcome.CONFIRMED) {
            cache.completed(event);
            return 1;
        }
        return 0; // The atomic mutation retains an outbox entry for retry.
    }

    static Long parsePostId(String key) {
        String prefix = "post:like:{";
        String suffix = "}:meta";
        if (key == null || !key.startsWith(prefix) || !key.endsWith(suffix)) return null;
        try {
            return Long.parseLong(key.substring(prefix.length(), key.length() - suffix.length()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
