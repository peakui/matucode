package com.peakui.post.like;

public record LikeMutationResult(boolean changed, int likeCount, long version, String eventId) {
}
