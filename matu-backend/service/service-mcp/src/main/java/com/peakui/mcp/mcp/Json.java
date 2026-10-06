package com.peakui.mcp.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.mcp.McpToolException;

/** Serializes a downstream JSON payload into the text a tool returns. */
public final class Json {

    private Json() {
    }

    public static String text(ObjectMapper mapper, JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new McpToolException("结果序列化失败: " + e.getMessage());
        }
    }
}
