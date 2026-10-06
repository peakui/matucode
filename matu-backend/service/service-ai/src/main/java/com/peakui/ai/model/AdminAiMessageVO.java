package com.peakui.ai.model;

import java.time.LocalDateTime;

public record AdminAiMessageVO(
        Long id,
        String conversationId,
        String userId,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
