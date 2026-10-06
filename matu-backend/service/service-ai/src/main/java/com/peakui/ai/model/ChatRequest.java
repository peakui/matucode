package com.peakui.ai.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        String conversationId,
        @NotBlank(message = "消息不能为空")
        @Size(max = 12000, message = "消息过长")
        String message,
        Boolean stream,
        String scene
) {
    public boolean streaming() {
        return stream == null || stream;
    }
}
