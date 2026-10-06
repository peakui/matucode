package com.peakui.ai.model;

import java.time.LocalDateTime;

public record AiConversationVO(
        Long id,
        String conversationId,
        String title,
        String scene,
        Integer messageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
