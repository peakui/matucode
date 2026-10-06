package com.peakui.ai.model;

import java.util.List;

/**
 * One turn of a streamed chat completion. {@code Token} events carry visible
 * text; a single terminal {@code Completed} reports any tool calls the model
 * requested and the upstream finish reason.
 */
public sealed interface ChatStreamEvent {

    record Token(String text) implements ChatStreamEvent {
    }

    record Completed(List<ToolCall> toolCalls, String finishReason) implements ChatStreamEvent {
    }
}
