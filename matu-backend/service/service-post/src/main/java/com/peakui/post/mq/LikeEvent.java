package com.peakui.post.mq;

/** IDs are encoded as JSON strings by Lua to preserve Snowflake precision. */
public record LikeEvent(String eventId, Long postId, Long userId, boolean liked,
                        long version, long occurredAt) { }
