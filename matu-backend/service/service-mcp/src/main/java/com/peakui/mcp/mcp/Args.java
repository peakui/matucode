package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.peakui.mcp.McpToolException;

/** Lenient readers for the JSON argument object the model passes to a tool. */
public final class Args {

    private Args() {
    }

    public static String text(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value.trim();
    }

    public static String requireText(JsonNode args, String field) {
        String value = text(args, field);
        if (value == null) {
            throw new McpToolException("缺少必填参数: " + field);
        }
        return value;
    }

    public static Long number(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        String value = node.asText().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new McpToolException("参数 " + field + " 必须是整数");
        }
    }

    public static Long requireNumber(JsonNode args, String field) {
        Long value = number(args, field);
        if (value == null) {
            throw new McpToolException("缺少必填参数: " + field);
        }
        return value;
    }

    public static Integer integer(JsonNode args, String field) {
        Long value = number(args, field);
        return value == null ? null : value.intValue();
    }
}
