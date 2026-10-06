package com.peakui.ai.model;

import java.time.LocalDateTime;

public record AdminAiConversationVO(
        Long id,
        String conversationId,
        String userId,
        String title,
        String scene,
        Integer messageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
