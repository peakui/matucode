package com.peakui.post.like;

import java.util.List;

/** All keys used by a script share a Redis Cluster hash slot. */
public final class LikeKeys {
    public static final String INVALIDATIONS = "post:like:invalidate";
    private LikeKeys() { }
    public static String base(long postId) { return "post:like:{" + postId + "}:"; }
    public static String meta(long postId) { return base(postId) + "meta"; }
    public static String users(long postId) { return base(postId) + "users"; }
    public static String versions(long postId) { return base(postId) + "versions"; }
    public static String outbox(long postId) { return base(postId) + "outbox"; }
    public static String lock(long postId) { return base(postId) + "init-lock"; }
    public static List<String> keys(long postId) {
        return List.of(users(postId), meta(postId), versions(postId), outbox(postId));
    }
}
