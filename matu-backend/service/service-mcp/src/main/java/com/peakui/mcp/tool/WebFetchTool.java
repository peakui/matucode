package com.peakui.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.mcp.McpProperties;
import com.peakui.mcp.McpToolException;
import com.peakui.mcp.mcp.Args;
import com.peakui.mcp.mcp.McpTool;
import com.peakui.mcp.mcp.Schemas;
import com.peakui.mcp.webfetch.HtmlTextExtractor;
import com.peakui.mcp.webfetch.SsrfGuard;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/** Fetches a public web page and returns its readable text. */
@Component
public class WebFetchTool implements McpTool {

    private final SsrfGuard guard;
    private final McpProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public WebFetchTool(SsrfGuard guard, McpProperties properties, ObjectMapper mapper) {
        this.guard = guard;
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getWebFetch().getTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String name() {
        return "web_fetch";
    }

    @Override
    public String description() {
        return "抓取一个公开网页并返回抽取后的正文文本，用于回答需要站外资料的问题。只能访问公网 http/https 地址。";
    }

    @Override
    public JsonNode inputSchema() {
        return Schemas.of(mapper)
                .string("url", "要抓取的完整URL（http/https）", true)
                .build();
    }

    @Override
    public String call(JsonNode arguments) {
        McpProperties.WebFetch config = properties.getWebFetch();
        String rawUrl = Args.requireText(arguments, "url");
        URI uri = guard.validate(rawUrl);
        for (int hop = 0; hop <= config.getMaxRedirects(); hop++) {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .header("User-Agent", "matu-mcp/1.0 (+https://www.example.com)")
                    .header("Accept", "text/html,text/plain,application/json;q=0.9")
                    .GET()
                    .build();
            HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (IOException e) {
                throw new McpToolException("请求失败: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new McpToolException("请求被中断", e);
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                Optional<String> location = response.headers().firstValue("Location");
                close(response.body());
                if (location.isEmpty()) {
                    throw new McpToolException("重定向缺少 Location");
                }
                uri = guard.validate(uri.resolve(location.get()).toString());
                continue;
            }
            if (status >= 400) {
                close(response.body());
                throw new McpToolException("远端返回 HTTP " + status);
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!isTextual(contentType)) {
                close(response.body());
                throw new McpToolException("不支持的内容类型: " + contentType);
            }
            byte[] body = readCapped(response.body(), config.getMaxBytes());
            String text = contentType.toLowerCase(Locale.ROOT).contains("html")
                    ? HtmlTextExtractor.extract(body, charsetOf(contentType))
                    : new String(body, charsetOf(contentType));
            return truncate(text.trim(), config.getMaxChars());
        }
        throw new McpToolException("重定向次数过多");
    }

    private static boolean isTextual(String contentType) {
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.isEmpty() || type.startsWith("text/") || type.contains("json") || type.contains("xml");
    }

    private static Charset charsetOf(String contentType) {
        int index = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
        if (index < 0) {
            return StandardCharsets.UTF_8;
        }
        String name = contentType.substring(index + "charset=".length()).split("[;,]")[0].trim().replace("\"", "");
        try {
            return name.isEmpty() ? StandardCharsets.UTF_8 : Charset.forName(name);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static byte[] readCapped(InputStream in, long maxBytes) {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while (total < maxBytes && (read = in.read(buffer)) != -1) {
                int toWrite = (int) Math.min(read, maxBytes - total);
                out.write(buffer, 0, toWrite);
                total += toWrite;
                if (toWrite < read) {
                    break;
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new McpToolException("读取响应失败: " + e.getMessage(), e);
        }
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n…(内容已截断)";
    }

    private static void close(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // best effort
        }
    }
}
