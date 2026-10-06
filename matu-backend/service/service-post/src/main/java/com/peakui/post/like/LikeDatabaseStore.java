package com.peakui.post.like;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.peakui.post.exception.PostException;
import com.peakui.post.mq.LikeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LikeDatabaseStore {
    private final JdbcTemplate jdbc;

    public record VersionRow(long userId, long version) { }
    public record State(boolean liked, long version) { }

    /** Snapshot is isolated from any HTTP transaction; bootstrap uses bounded pages. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public long snapshot(long postId, Consumer<List<Long>> users, BiConsumer<Long, Long> versions) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE id=? AND status<>3", Integer.class, postId) == 0) {
            throw new PostException("帖子不存在");
        }
        long afterUsers = 0;
        while (true) {
            final long cursor = afterUsers;
            List<Long> page = users(postId, cursor, 500);
            if (page.isEmpty()) break;
            users.accept(page);
            afterUsers = page.get(page.size() - 1);
        }
        long afterVersions = 0;
        long max = 0;
        while (true) {
            final long cursor = afterVersions;
            List<VersionRow> page = jdbc.query("SELECT user_id,version FROM post_like_state WHERE post_id=? AND user_id>? ORDER BY user_id LIMIT 500",
                    (rs, n) -> new VersionRow(rs.getLong(1), rs.getLong(2)), postId, cursor);
            if (page.isEmpty()) break;
            for (VersionRow row : page) {
                versions.accept(row.userId(), row.version());
                max = Math.max(max, row.version());
            }
            afterVersions = page.get(page.size() - 1).userId();
        }
        return max;
    }

    public List<Long> users(long postId, long after, int size) {
        if (size == Integer.MAX_VALUE) {
            return jdbc.queryForList("SELECT DISTINCT user_id FROM post_likes WHERE post_id=? AND user_id>? ORDER BY user_id",
                    Long.class, postId, after);
        }
        return jdbc.queryForList("SELECT DISTINCT user_id FROM post_likes WHERE post_id=? AND user_id>? ORDER BY user_id LIMIT ?",
                Long.class, postId, after, size);
    }

    public int count(long postId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id=?", Integer.class, postId);
    }
    public boolean liked(long postId, long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post_likes WHERE post_id=? AND user_id=?", Integer.class, postId, userId) > 0;
    }

    /** Lock the parent row: pair version, membership and counter commit together across instances. */
    @Transactional(rollbackFor = Exception.class)
    public void apply(LikeEvent event) {
        List<Long> posts = jdbc.queryForList("SELECT id FROM posts WHERE id=? AND status<>3 FOR UPDATE", Long.class, event.postId());
        if (posts.isEmpty()) return; // physically removed posts must never be resurrected
        List<State> states = jdbc.query("SELECT liked,version FROM post_like_state WHERE post_id=? AND user_id=?",
                (rs, n) -> new State(rs.getBoolean(1), rs.getLong(2)), event.postId(), event.userId());
        if (!states.isEmpty() && states.get(0).version() >= event.version()) return;
        boolean old = liked(event.postId(), event.userId());
        if (old != event.liked()) {
            if (event.liked()) {
                jdbc.update("INSERT INTO post_likes(id,post_id,user_id,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP)",
                        IdWorker.getId(), event.postId(), event.userId());
            } else {
                jdbc.update("DELETE FROM post_likes WHERE post_id=? AND user_id=?", event.postId(), event.userId());
            }
            jdbc.update("UPDATE posts SET like_count=GREATEST(COALESCE(like_count,0)+?,0) WHERE id=?",
                    event.liked() ? 1 : -1, event.postId());
        }
        jdbc.update("INSERT INTO post_like_state(post_id,user_id,version,liked) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE version=VALUES(version),liked=VALUES(liked)",
                event.postId(), event.userId(), event.version(), event.liked());
    }

    @Transactional
    public void repairCount(long postId) {
        if (!jdbc.queryForList("SELECT id FROM posts WHERE id=? AND status<>3 FOR UPDATE", Long.class, postId).isEmpty()) {
            jdbc.update("UPDATE posts SET like_count=? WHERE id=?", count(postId), postId);
        }
    }
}
