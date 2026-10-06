package com.peakui.post.mq;

/**
 * 帖子发布后的 AI 自动化事件。
 */
public record PostPublishedEvent(Long postId, String title, String content) {
}
