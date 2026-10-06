package com.peakui.post.like;

import com.peakui.post.mq.LikeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeDatabaseStoreTest {

    private static final long POST_ID = 42L;
    private static final long USER_ID = 7L;
    private static final String LOCK_POST = "SELECT id FROM posts WHERE id=? AND status<>3 FOR UPDATE";
    private static final String LOAD_STATE =
            "SELECT liked,version FROM post_like_state WHERE post_id=? AND user_id=?";
    private static final String LOAD_LIKE =
            "SELECT COUNT(*) FROM post_likes WHERE post_id=? AND user_id=?";
    private static final String INSERT_LIKE =
            "INSERT INTO post_likes(id,post_id,user_id,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP)";
    private static final String DELETE_LIKE =
            "DELETE FROM post_likes WHERE post_id=? AND user_id=?";
    private static final String UPDATE_COUNT =
            "UPDATE posts SET like_count=GREATEST(COALESCE(like_count,0)+?,0) WHERE id=?";
    private static final String UPSERT_STATE =
            "INSERT INTO post_like_state(post_id,user_id,version,liked) VALUES(?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE version=VALUES(version),liked=VALUES(liked)";

    private JdbcTemplate jdbc;
    private LikeDatabaseStore store;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        store = new LikeDatabaseStore(jdbc);
        when(jdbc.queryForList(eq(LOCK_POST), eq(Long.class), eq(POST_ID)))
                .thenReturn(List.of(POST_ID));
    }

    @Test
    void newerLikeWritesMembershipCounterAndVersionInOrder() {
        when(jdbc.query(eq(LOAD_STATE), anyRowMapper(), eq(POST_ID), eq(USER_ID)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(eq(LOAD_LIKE), eq(Integer.class), eq(POST_ID), eq(USER_ID)))
                .thenReturn(0);

        store.apply(new LikeEvent("event-7", POST_ID, USER_ID, true, 7L, 100L));

        var order = inOrder(jdbc);
        order.verify(jdbc).queryForList(eq(LOCK_POST), eq(Long.class), eq(POST_ID));
        order.verify(jdbc).query(eq(LOAD_STATE), anyRowMapper(), eq(POST_ID), eq(USER_ID));
        order.verify(jdbc).queryForObject(eq(LOAD_LIKE), eq(Integer.class), eq(POST_ID), eq(USER_ID));
        order.verify(jdbc).update(eq(INSERT_LIKE), anyLong(), eq(POST_ID), eq(USER_ID));
        order.verify(jdbc).update(eq(UPDATE_COUNT), eq(1), eq(POST_ID));
        order.verify(jdbc).update(eq(UPSERT_STATE), eq(POST_ID), eq(USER_ID), eq(7L), eq(true));
    }

    @Test
    void sameVersionIsIdempotentEvenWhenPayloadDiffers() {
        when(jdbc.query(eq(LOAD_STATE), anyRowMapper(), eq(POST_ID), eq(USER_ID)))
                .thenReturn(List.of(new LikeDatabaseStore.State(true, 7L)));

        store.apply(new LikeEvent("duplicate", POST_ID, USER_ID, false, 7L, 101L));

        verify(jdbc, never()).queryForObject(eq(LOAD_LIKE), eq(Integer.class), eq(POST_ID), eq(USER_ID));
        verify(jdbc, never()).update(eq(INSERT_LIKE), anyLong(), eq(POST_ID), eq(USER_ID));
        verify(jdbc, never()).update(eq(DELETE_LIKE), eq(POST_ID), eq(USER_ID));
        verify(jdbc, never()).update(eq(UPDATE_COUNT), any(), eq(POST_ID));
        verify(jdbc, never()).update(eq(UPSERT_STATE), any(), any(), any(), any());
    }

    @Test
    void olderEventIsIgnoredAfterNewerVersion() {
        when(jdbc.query(eq(LOAD_STATE), anyRowMapper(), eq(POST_ID), eq(USER_ID)))
                .thenReturn(List.of(new LikeDatabaseStore.State(false, 9L)));

        store.apply(new LikeEvent("late", POST_ID, USER_ID, true, 8L, 102L));

        verify(jdbc, never()).queryForObject(eq(LOAD_LIKE), eq(Integer.class), eq(POST_ID), eq(USER_ID));
        verify(jdbc, never()).update(eq(INSERT_LIKE), anyLong(), eq(POST_ID), eq(USER_ID));
        verify(jdbc, never()).update(eq(UPDATE_COUNT), any(), eq(POST_ID));
        verify(jdbc, never()).update(eq(UPSERT_STATE), any(), any(), any(), any());
    }

    @Test
    void newerUnlikeDeletesMembershipDecrementsCounterAndKeepsTombstone() {
        when(jdbc.query(eq(LOAD_STATE), anyRowMapper(), eq(POST_ID), eq(USER_ID)))
                .thenReturn(List.of(new LikeDatabaseStore.State(true, 4L)));
        when(jdbc.queryForObject(eq(LOAD_LIKE), eq(Integer.class), eq(POST_ID), eq(USER_ID)))
                .thenReturn(1);

        store.apply(new LikeEvent("unlike-5", POST_ID, USER_ID, false, 5L, 103L));

        verify(jdbc).update(eq(DELETE_LIKE), eq(POST_ID), eq(USER_ID));
        verify(jdbc).update(eq(UPDATE_COUNT), eq(-1), eq(POST_ID));
        verify(jdbc).update(eq(UPSERT_STATE), eq(POST_ID), eq(USER_ID), eq(5L), eq(false));
    }

    @Test
    void countReadsLikeRecordsForPost() {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id=?";
        when(jdbc.queryForObject(eq(sql), eq(Integer.class), eq(POST_ID))).thenReturn(3);

        assertEquals(3, store.count(POST_ID));
        verify(jdbc).queryForObject(eq(sql), eq(Integer.class), eq(POST_ID));
    }

    @Test
    void applyUsesOneRollbackCapableTransactionBoundary() throws NoSuchMethodException {
        Method apply = LikeDatabaseStore.class.getMethod("apply", LikeEvent.class);
        Transactional transaction = apply.getAnnotation(Transactional.class);

        assertTrue(transaction != null);
        assertEquals(List.of(Exception.class), List.of(transaction.rollbackFor()));
    }

    @SuppressWarnings("unchecked")
    private static RowMapper<LikeDatabaseStore.State> anyRowMapper() {
        return (RowMapper<LikeDatabaseStore.State>) (RowMapper<?>) any(RowMapper.class);
    }
}
