package com.peakui.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class McpClient {
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public List<ToolDescriptor> tools() {
        if (!properties.getMcp().isEnabled()) return List.of();
        List<ToolDescriptor> result = new ArrayList<>();
        for (AiProperties.Server server : properties.getMcp().getServers()) {
            if (!server.isEnabled() || !StringUtils.hasText(server.getUrl())) continue;
            try {
                JsonNode response = request(server, "tools/list", Map.of());
                JsonNode tools = response.path("result").path("tools");
                if (tools.isArray()) {
                    for (JsonNode tool : tools) {
                        String name = tool.path("name").asText();
                        if (server.getAllowTools().isEmpty() || server.getAllowTools().contains(name)) {
                            result.add(new ToolDescriptor(server.getName(), name, tool.path("description").asText(""), tool.path("inputSchema")));
                        }
                    }
                }
            } catch (Exception ignored) {
                // 单个 MCP 服务不可用时，不拖垮主对话链路。
            }
        }
        return result;
    }

    public JsonNode call(String serverName, String toolName, Map<String, Object> arguments) {
        AiProperties.Server server = properties.getMcp().getServers().stream()
                .filter(item -> item.isEnabled() && item.getName().equals(serverName))
                .findFirst().orElseThrow(() -> new AiBusinessException(403, "MCP 服务不在允许列表中"));
        if (!server.getAllowTools().isEmpty() && !server.getAllowTools().contains(toolName)) {
            throw new AiBusinessException(403, "MCP 工具不在允许列表中");
        }
        if (!StringUtils.hasText(server.getUrl())) throw new AiBusinessException(503, "MCP 服务未配置");
        return request(server, "tools/call", Map.of("name", toolName, "arguments", arguments));
    }

    private JsonNode request(AiProperties.Server server, String method, Map<String, Object> params) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("jsonrpc", "2.0");
            body.put("id", System.nanoTime());
            body.put("method", method);
            body.set("params", objectMapper.valueToTree(params));
            return aiWebClient.post().uri(server.getUrl())
                    .headers(headers -> {
                        if (StringUtils.hasText(server.getApiKey())) headers.setBearerAuth(server.getApiKey());
                    })
                    .bodyValue(body)
                    .retrieve().bodyToMono(JsonNode.class)
                    .block(Duration.ofMillis(properties.getMcp().getRequestTimeoutMs()));
        } catch (AiBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new AiBusinessException(502, "MCP 工具服务暂时不可用");
        }
    }

    public record ToolDescriptor(String server, String name, String description, JsonNode inputSchema) {
    }
}
