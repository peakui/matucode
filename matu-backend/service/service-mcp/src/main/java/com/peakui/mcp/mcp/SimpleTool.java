package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

/** {@link McpTool} backed by a lambda, so tool definitions stay compact. */
public record SimpleTool(String name, String description, JsonNode inputSchema,
                         Function<JsonNode, String> handler) implements McpTool {

    @Override
    public String call(JsonNode arguments) {
        return handler.apply(arguments);
    }
}
