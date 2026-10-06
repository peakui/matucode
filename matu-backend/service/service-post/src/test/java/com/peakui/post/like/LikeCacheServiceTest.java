package com.peakui.post.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LikeCacheServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LikeDatabaseStore database = mock(LikeDatabaseStore.class);
    private HashOperations<String, String, String> hashes;
    private LikeCacheService cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        hashes = mock(HashOperations.class);
        when(redis.<String, String>opsForHash()).thenReturn(hashes);
        cache = new LikeCacheService(redis, database, new ObjectMapper(), new HeavyKeeper(128, 4),
                100, 10, 2, 30);
        when(hashes.get(LikeKeys.meta(42), "ready")).thenReturn("1");
        when(hashes.get(LikeKeys.meta(42), "count")).thenReturn("5");
    }

    @Test
    void coldReadsUseRedisAndHotReadsUseLocalCache() {
        assertEquals(5, cache.getLikeCount(42L));
        assertEquals(5, cache.getLikeCount(42L));
        assertEquals(5, cache.getLikeCount(42L));
        verify(hashes, times(2)).get(LikeKeys.meta(42), "count");
        assertEquals(3, cache.stats().requestCount());
        assertEquals(1, cache.stats().hitCount());
    }

    @Test
    void invalidationMakesNextReadReturnRedisCount() {
        cache.getLikeCount(42L);
        cache.getLikeCount(42L);
        cache.invalidate(42);
        when(hashes.get(LikeKeys.meta(42), "count")).thenReturn("6");
        assertEquals(6, cache.getLikeCount(42L));
    }

    @Test
    void redisOutageFallsBackAndTemporarilySkipsRedis() {
        when(hashes.get(LikeKeys.meta(42), "ready"))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));
        when(database.count(42)).thenReturn(3);
        assertEquals(3, cache.getLikeCount(42L));
        assertEquals(3, cache.getLikeCount(42L));
        verify(hashes).get(LikeKeys.meta(42), "ready");
        verify(database, times(2)).count(42);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initializationContentionFallsBackForRead() {
        when(hashes.get(LikeKeys.meta(42), "ready")).thenReturn(null);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(database.count(42)).thenReturn(4);
        assertEquals(4, cache.getLikeCount(42L));
        verify(database).count(42);
    }

    @Test
    void incompleteMetadataFallsBackForRead() {
        when(hashes.get(LikeKeys.meta(42), "count")).thenReturn(null);
        when(database.count(42)).thenReturn(4);
        assertEquals(4, cache.getLikeCount(42L));
    }

    @Test
    void anonymousUserIsNeverLiked() {
        assertFalse(cache.hasLiked(42L, null));
        verifyNoInteractions(database);
    }
}
