package com.peakui.ai.model;

import java.util.List;

public record ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream, Integer max_tokens,
                                    List<Tool> tools, String tool_choice) {

    public ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream, Integer max_tokens) {
        this(model, messages, stream, max_tokens, null, null);
    }
}
