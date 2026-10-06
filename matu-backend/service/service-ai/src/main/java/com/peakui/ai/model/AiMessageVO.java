package com.peakui.ai.model;

import java.time.LocalDateTime;

public record AiMessageVO(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
