package com.peakui.post.reconcile;

import com.peakui.post.like.*;
import com.peakui.post.mq.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LikeReconciliationJobTest {
    private final LikeCacheService cache = mock(LikeCacheService.class);
    private final LikeDatabaseStore database = mock(LikeDatabaseStore.class);
    private final LikeEventPublisher publisher = mock(LikeEventPublisher.class);
    private final LikeReconciliationJob job = new LikeReconciliationJob(
            mock(StringRedisTemplate.class), cache, database, publisher);

    @Test
    void parsesActualHashTaggedKeyAndRejectsOtherKeys() {
        assertEquals(42L, LikeReconciliationJob.parsePostId(LikeKeys.meta(42)));
        assertNull(LikeReconciliationJob.parsePostId(LikeKeys.users(42)));
        assertNull(LikeReconciliationJob.parsePostId("post:like:{bad}:meta"));
        assertNull(LikeReconciliationJob.parsePostId(null));
    }

    @Test
    void redisLikeMissingFromDatabaseProducesLikeEvent() {
        verifyRepair(true);
    }

    @Test
    void databaseLikeMissingFromRedisProducesUnlikeEvent() {
        verifyRepair(false);
    }

    private void verifyRepair(boolean liked) {
        var snapshot = new LikeCacheService.Snapshot(liked, 7L);
        when(cache.snapshot(42, 8)).thenReturn(snapshot);
        when(database.liked(42, 8)).thenReturn(!liked);
        when(cache.reconcileMutation(42, 8, snapshot)).thenReturn(new LikeMutationResult(true, 1, 8, "repair"));
        when(publisher.publish(any())).thenReturn(LikeEventPublisher.Outcome.CONFIRMED);
        assertEquals(1, job.reconcileUser(42, 8));
        verify(publisher).publish(argThat(event -> event.liked() == liked && event.version() == 8));
        verify(cache).completed(argThat(event -> event.eventId().equals("repair")));
    }

    @Test
    void concurrentUserWriteCancelsStaleRepair() {
        var snapshot = new LikeCacheService.Snapshot(true, 7L);
        when(cache.snapshot(42, 8)).thenReturn(snapshot);
        when(cache.reconcileMutation(42, 8, snapshot)).thenReturn(null);
        assertEquals(0, job.reconcileUser(42, 8));
        verifyNoInteractions(publisher);
    }

    @Test
    void equalStatesDoNotCreateEvents() {
        when(cache.snapshot(42, 8)).thenReturn(new LikeCacheService.Snapshot(false, 7L));
        assertEquals(0, job.reconcileUser(42, 8));
        verify(cache, never()).reconcileMutation(anyLong(), anyLong(), any());
        verifyNoInteractions(publisher);
    }

    @Test
    void unknownConfirmRetainsOutbox() {
        var snapshot = new LikeCacheService.Snapshot(true, 7L);
        when(cache.snapshot(42, 8)).thenReturn(snapshot);
        when(cache.reconcileMutation(42, 8, snapshot)).thenReturn(new LikeMutationResult(true, 1, 8, "repair"));
        when(publisher.publish(any())).thenReturn(LikeEventPublisher.Outcome.UNKNOWN);
        assertEquals(0, job.reconcileUser(42, 8));
        verify(cache, never()).completed(any());
    }
}
