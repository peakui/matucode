package com.peakui.ai.model;

/**
 * An OpenAI-style tool call. Serialized as {@code tool_calls[]} on an assistant
 * message (arguments is a JSON string, per the wire spec), and produced by
 * parsing the streamed {@code delta.tool_calls} fragments.
 */
public record ToolCall(String id, String type, FunctionCall function) {

    public record FunctionCall(String name, String arguments) {
    }
}
