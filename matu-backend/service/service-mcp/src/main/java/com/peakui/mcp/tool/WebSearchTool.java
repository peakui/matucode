package com.peakui.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peakui.mcp.McpProperties;
import com.peakui.mcp.McpToolException;
import com.peakui.mcp.mcp.Args;
import com.peakui.mcp.mcp.McpTool;
import com.peakui.mcp.mcp.Schemas;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Searches the public web through the Bocha Web Search API. */
@Component
public class WebSearchTool implements McpTool {

    private final McpProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public WebSearchTool(McpProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getWebSearch().getTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "联网搜索最新公开信息，返回网页标题、链接与摘要。用于回答需要实时或站外资料的问题。";
    }

    @Override
    public JsonNode inputSchema() {
        return Schemas.of(mapper)
                .string("query", "搜索关键词", true)
                .integer("count", "返回条数(1-10)", false)
                .string("freshness", "时效:noLimit/oneDay/oneWeek/oneMonth/oneYear", false)
                .build();
    }

    @Override
    public String call(JsonNode arguments) {
        McpProperties.WebSearch config = properties.getWebSearch();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new McpToolException("未配置搜索服务密钥");
        }
        String query = Args.requireText(arguments, "query");
        int count = resolveCount(Args.integer(arguments, "count"), config.getCount());
        String freshness = Args.text(arguments, "freshness");
        if (freshness == null) {
            freshness = config.getFreshness();
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("query", query);
        body.put("count", count);
        body.put("freshness", freshness);
        body.put("summary", config.isSummary());

        HttpRequest request = HttpRequest.newBuilder(URI.create(config.getEndpoint()))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new McpToolException("搜索请求失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McpToolException("搜索请求被中断", e);
        }
        if (response.statusCode() >= 400) {
            throw new McpToolException("搜索服务返回 HTTP " + response.statusCode());
        }
        return format(response.body(), config.getMaxChars());
    }

    private static int resolveCount(Integer requested, int fallback) {
        int value = requested == null ? fallback : requested;
        return Math.max(1, Math.min(10, value));
    }

    private String format(String rawBody, int maxChars) {
        JsonNode root;
        try {
            root = mapper.readTree(rawBody);
        } catch (Exception e) {
            throw new McpToolException("搜索响应格式错误");
        }
        int code = root.path("code").asInt(200);
        if (code != 200) {
            String message = root.path("msg").asText("");
            throw new McpToolException("搜索服务返回错误" + (message.isEmpty() ? "" : ": " + message));
        }
        JsonNode pages = root.path("data").path("webPages").path("value");
        if (!pages.isArray() || pages.isEmpty()) {
            return "未找到相关结果。";
        }
        StringBuilder out = new StringBuilder();
        int index = 1;
        for (JsonNode page : pages) {
            String title = text(page, "name");
            String url = text(page, "url");
            String snippet = text(page, "summary");
            if (snippet == null) {
                snippet = text(page, "snippet");
            }
            String date = text(page, "dateLastCrawled");
            out.append(index++).append(". ").append(title == null ? "(无标题)" : title).append("\n");
            if (url != null) {
                out.append("   ").append(url).append("\n");
            }
            if (snippet != null) {
                out.append("   ").append(snippet).append("\n");
            }
            if (date != null) {
                out.append("   时间: ").append(date).append("\n");
            }
            out.append("\n");
        }
        return truncate(out.toString().trim(), maxChars);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n…(内容已截断)";
    }
}
