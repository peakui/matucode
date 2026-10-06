package com.peakui.post.like;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.peakui.post.exception.PostException;
import com.peakui.post.mq.LikeEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class LikeCacheService {
    public record Snapshot(boolean liked, long version) { }
    private final StringRedisTemplate redis;
    private final LikeDatabaseStore database;
    private final ObjectMapper json;
    private final HeavyKeeper heavyKeeper;
    private final int threshold;
    private final long lockSeconds;
    private final long localTtlMillis;
    private final long localJitterMillis;
    private final Cache<Long, Integer> local;
    private final Cache<Long, Boolean> missing = Caffeine.newBuilder().maximumSize(10000)
            .expireAfterWrite(Duration.ofSeconds(15)).build();
    private final Semaphore databaseSlots = new Semaphore(8);
    private final Object[] stripes = new Object[256];
    private final AtomicLong redisRetryAt = new AtomicLong();

    public LikeCacheService(StringRedisTemplate redis, LikeDatabaseStore database, ObjectMapper json,
            HeavyKeeper heavyKeeper,
            @Value("${post.like.l1.maximum-size:10000}") long maxSize,
            @Value("${post.like.l1.expire-after-write-seconds:3}") long ttl,
            @Value("${post.like.hot-key.threshold:20}") int threshold,
            @Value("${post.like.init-lock-seconds:30}") long lockSeconds) {
        this.redis = redis;
        this.database = database;
        this.json = json;
        this.heavyKeeper = heavyKeeper;
        this.threshold = Math.max(1, threshold);
        this.lockSeconds = Math.max(1, lockSeconds);
        this.localTtlMillis = Math.max(1, ttl) * 1000L;
        this.localJitterMillis = Math.min(1000L, Math.max(100L, localTtlMillis / 10));
        this.local = Caffeine.newBuilder().maximumSize(Math.max(1, maxSize)).recordStats()
                .expireAfter(new Expiry<Long, Integer>() {
                    @Override
                    public long expireAfterCreate(Long key, Integer value, long currentTime) {
                        long jitter = ThreadLocalRandom.current().nextLong(localJitterMillis + 1);
                        return Duration.ofMillis(localTtlMillis + jitter).toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(Long key, Integer value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(Long key, Integer value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                }).build();
        for (int i = 0; i < stripes.length; i++) stripes[i] = new Object();
    }

    public LikeMutationResult mutate(Long postId, Long userId, boolean liked) {
        ensureInitialized(postId);
        return change(postId, userId, liked, -1, false);
    }

    /** A newer user operation must never be undone by a failed earlier publish. */
    public void rollback(long postId, long userId, boolean originalLiked, long expectedVersion) {
        change(postId, userId, !originalLiked, expectedVersion, true);
    }

    /** Reconciliation must not overwrite a user operation newer than its snapshot. */
    public LikeMutationResult reconcileMutation(long postId, long userId, Snapshot expected) {
        return change(postId, userId, expected.liked(), expected.version(), true);
    }

    private LikeMutationResult change(long postId, long userId, boolean liked, long expectedVersion, boolean force) {
        String eventId = UUID.randomUUID().toString();
        String result = redis.execute(LikeRedisScript.MUTATE, LikeKeys.keys(postId),
                Long.toString(userId), liked ? "1" : "0", eventId, Long.toString(postId),
                Long.toString(System.currentTimeMillis()), Long.toString(expectedVersion), force ? "1" : "0");
        invalidate(postId);
        if ("STALE".equals(result)) return null;
        if (result == null || "UNINITIALIZED".equals(result)) throw new IllegalStateException("点赞状态未初始化，请重试");
        String[] parts = result.split(":", 4);
        return new LikeMutationResult("1".equals(parts[0]), Integer.parseInt(parts[1]), Long.parseLong(parts[2]),
                parts.length == 4 ? parts[3] : null);
    }

    public boolean hasLiked(Long postId, Long userId) {
        if (userId == null) return false;
        if (TransactionSynchronizationManager.isActualTransactionActive()) return database.liked(postId, userId);
        if (System.currentTimeMillis() < redisRetryAt.get()) return guarded(() -> database.liked(postId, userId));
        try {
            ensureInitialized(postId);
            return snapshot(postId, userId).liked();
        } catch (DataAccessException | IllegalStateException e) {
            redisRetryAt.set(System.currentTimeMillis() + 1000);
            return guarded(() -> database.liked(postId, userId));
        }
    }

    public int getLikeCount(Long postId) {
        int frequency = heavyKeeper.record("post:like:count:" + postId); // includes every L1 hit
        if (TransactionSynchronizationManager.isActualTransactionActive()) return database.count(postId);
        synchronized (stripe(postId)) {
            Integer hit = local.getIfPresent(postId);
            if (hit != null) return hit;
            if (System.currentTimeMillis() < redisRetryAt.get()) return guarded(() -> database.count(postId));
            try {
                ensureInitialized(postId);
                String raw = redis.<String, String>opsForHash().get(LikeKeys.meta(postId), "count");
                if (raw == null) throw new IllegalStateException("点赞缓存不完整，需要恢复");
                int count = Integer.parseInt(raw);
                if (frequency >= threshold) local.put(postId, count);
                return count;
            } catch (DataAccessException | IllegalStateException e) {
                redisRetryAt.set(System.currentTimeMillis() + 1000);
                return guarded(() -> database.count(postId));
            }
        }
    }

    public Snapshot snapshot(long postId, long userId) {
        String value = redis.execute(LikeRedisScript.SNAPSHOT, LikeKeys.keys(postId), Long.toString(userId));
        if (value == null || "UNINITIALIZED".equals(value)) throw new IllegalStateException("点赞状态尚未初始化");
        String[] parts = value.split(":");
        return new Snapshot("1".equals(parts[0]), Long.parseLong(parts[1]));
    }

    public LikeEvent pending(long postId, long userId) {
        String raw = redis.<String, String>opsForHash().get(LikeKeys.outbox(postId), Long.toString(userId));
        return raw == null ? null : decode(raw);
    }

    public LikeEvent decode(String raw) {
        try { return json.readValue(raw, LikeEvent.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("点赞待发送记录损坏", e); }
    }
    public void completed(LikeEvent event) {
        redis.execute(LikeRedisScript.COMPLETE, List.of(LikeKeys.outbox(event.postId())),
                event.userId().toString(), event.eventId());
    }
    public void invalidate(long postId) {
        synchronized (stripe(postId)) { local.invalidate(postId); }
    }
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() { return local.stats(); }
    private Object stripe(long id) { return stripes[Math.floorMod(Long.hashCode(id), stripes.length)]; }

    public void ensureInitialized(long postId) {
        if (missing.getIfPresent(postId) != null) throw new PostException("帖子不存在");
        if ("1".equals(redis.<String, String>opsForHash().get(LikeKeys.meta(postId), "ready"))) return;
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(LikeKeys.lock(postId), token, Duration.ofSeconds(lockSeconds)))) {
            throw new IllegalStateException("点赞缓存正在恢复，请稍后重试");
        }
        String tempUsers = LikeKeys.base(postId) + "temp-users:" + token;
        String tempVersions = LikeKeys.base(postId) + "temp-versions:" + token;
        try {
            if ("1".equals(redis.<String, String>opsForHash().get(LikeKeys.meta(postId), "ready"))) return;
            long max = guarded(() -> database.snapshot(postId, users -> {
                redis.opsForSet().add(tempUsers, users.stream().map(Object::toString).toArray(String[]::new));
                redis.expire(tempUsers, Duration.ofMinutes(5));
            }, (user, version) -> {
                redis.opsForHash().put(tempVersions, user.toString(), version.toString());
                redis.expire(tempVersions, Duration.ofMinutes(5));
            }));
            Long installed = redis.execute(LikeRedisScript.INSTALL, List.of(LikeKeys.lock(postId), LikeKeys.users(postId),
                    LikeKeys.meta(postId), LikeKeys.versions(postId), LikeKeys.outbox(postId), tempUsers, tempVersions), token, Long.toString(max));
            if (!Long.valueOf(1).equals(installed)) throw new IllegalStateException("点赞缓存恢复锁已过期，请重试");
        } catch (PostException e) {
            if ("帖子不存在".equals(e.getMessage())) missing.put(postId, true);
            throw e;
        } finally {
            redis.delete(List.of(tempUsers, tempVersions));
            redis.execute(LikeRedisScript.RELEASE_LOCK, List.of(LikeKeys.lock(postId)), token);
        }
    }

    private <T> T guarded(Supplier<T> query) {
        if (!databaseSlots.tryAcquire()) throw new PostException("点赞服务繁忙，请稍后重试");
        try { return query.get(); } finally { databaseSlots.release(); }
    }
}
