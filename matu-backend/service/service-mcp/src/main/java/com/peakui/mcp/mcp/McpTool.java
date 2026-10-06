package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One MCP tool. Implementations are collected by {@link ToolRegistry} and
 * advertised to the model through {@code tools/list}.
 */
public interface McpTool {

    /** Tool name, unique across the registry; this is what the model invokes. */
    String name();

    /** Natural-language description shown to the model. */
    String description();

    /** JSON Schema object describing the tool's arguments. */
    JsonNode inputSchema();

    /**
     * Execute the tool. {@code arguments} is the JSON object the model passed;
     * fields may be absent. Returns the text content put back into the model
     * context. Throwing {@link McpToolException} produces a tool-level error,
     * not a transport error.
     */
    String call(JsonNode arguments);
}
