package com.peakui.mcp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "service-mcp")
public class McpProperties {

    private Mcp mcp = new Mcp();
    private WebFetch webFetch = new WebFetch();
    private WebSearch webSearch = new WebSearch();
    private int downstreamTimeoutMs = 8000;

    @Data
    public static class Mcp {
        /** Shared secret the MCP client must present as "Authorization: Bearer <token>". */
        @NotBlank
        private String token;
    }

    @Data
    public static class WebFetch {
        /** Hard cap on downloaded bytes before the body is truncated. */
        private long maxBytes = 1_048_576L;
        /** Hard cap on extracted characters returned to the model. */
        private int maxChars = 20_000;
        private int timeoutMs = 8000;
        private int maxRedirects = 3;
    }

    @Data
    public static class WebSearch {
        /** Bocha API key. When blank the tool reports that search is not configured. */
        private String apiKey;
        private String endpoint = "https://api.bochaai.com/v1/web-search";
        /** Default result count when the caller does not specify one. */
        private int count = 8;
        /** Default freshness window: noLimit / oneDay / oneWeek / oneMonth / oneYear. */
        private String freshness = "noLimit";
        /** Ask Bocha to return a generated summary in addition to the snippet. */
        private boolean summary = true;
        private int timeoutMs = 8000;
        /** Hard cap on characters returned to the model. */
        private int maxChars = 8000;
    }
}
