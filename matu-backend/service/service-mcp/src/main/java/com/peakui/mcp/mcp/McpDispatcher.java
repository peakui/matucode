package com.peakui.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peakui.mcp.McpToolException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Minimal JSON-RPC 2.0 dispatcher for the MCP methods the service-ai client
 * uses. It is stateless: the client in {@code com.peakui.ai.mcp.McpClient} posts
 * {@code tools/list} / {@code tools/call} directly without an initialize
 * handshake, and reads {@code result} from the response as-is.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpDispatcher {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "matu-mcp";
    private static final String SERVER_VERSION = "1.0.0";

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper;

    /** Returns the JSON-RPC response, or {@code null} for notifications (no reply expected). */
    public JsonNode dispatch(JsonNode request) {
        String method = request.path("method").asText("");
        JsonNode id = request.get("id");
        if (method.startsWith("notifications/")) {
            return null;
        }
        return switch (method) {
            case "initialize" -> result(id, initializeResult());
            case "ping" -> result(id, objectMapper.createObjectNode());
            case "tools/list" -> result(id, toolsList());
            case "tools/call" -> result(id, toolsCall(request.path("params")));
            default -> error(id, -32601, "Method not found: " + method);
        };
    }

    private ObjectNode initializeResult() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.putObject("capabilities").putObject("tools");
        result.putObject("serverInfo").put("name", SERVER_NAME).put("version", SERVER_VERSION);
        return result;
    }

    private ObjectNode toolsList() {
        ArrayNode tools = objectMapper.createArrayNode();
        for (McpTool tool : registry.all()) {
            ObjectNode node = tools.addObject();
            node.put("name", tool.name());
            node.put("description", tool.description());
            node.set("inputSchema", tool.inputSchema());
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", tools);
        return result;
    }

    private ObjectNode toolsCall(JsonNode params) {
        String name = params.path("name").asText("");
        JsonNode arguments = params.path("arguments");
        if (arguments.isMissingNode() || arguments.isNull()) {
            arguments = objectMapper.createObjectNode();
        }
        try {
            String text = registry.require(name).call(arguments);
            return content(text, false);
        } catch (McpToolException e) {
            return content(e.getMessage(), true);
        } catch (RuntimeException e) {
            log.warn("MCP tool {} failed", name, e);
            return content("工具执行失败: " + e.getMessage(), true);
        }
    }

    private ObjectNode content(String text, boolean isError) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", "text");
        block.put("text", text == null ? "" : text);
        ObjectNode result = objectMapper.createObjectNode();
        result.set("content", objectMapper.createArrayNode().add(block));
        result.put("isError", isError);
        return result;
    }

    private ObjectNode result(JsonNode id, JsonNode payload) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? objectMapper.nullNode() : id);
        response.set("result", payload);
        return response;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? objectMapper.nullNode() : id);
        response.putObject("error").put("code", code).put("message", message);
        return response;
    }
}
