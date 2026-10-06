package com.peakui.ai.model;

import java.util.List;

/**
 * A chat message. {@code tool_calls} is set on an assistant turn that requests
 * tools; {@code tool_call_id} links a {@code role:"tool"} result back to the
 * call. Nulls are dropped by the global non_null Jackson inclusion, so an
 * ordinary message serializes exactly as before.
 */
public record ChatMessage(String role, String content, List<ToolCall> tool_calls, String tool_call_id) {

    public ChatMessage(String role, String content) {
        this(role, content, null, null);
    }

    public static ChatMessage assistantToolCalls(List<ToolCall> calls) {
        return new ChatMessage("assistant", null, calls, null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }
}
