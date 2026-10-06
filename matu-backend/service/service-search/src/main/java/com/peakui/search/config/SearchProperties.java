package com.peakui.search.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    @Valid
    private Elasticsearch elasticsearch = new Elasticsearch();
    @Valid
    private Sync sync = new Sync();

    @Data
    public static class Elasticsearch {
        @NotBlank
        private String url = "http://localhost:9200";
        // A stable alias, never the name of a physical index.
        @Pattern(regexp = "[a-z][a-z0-9_-]{0,63}")
        private String index = "matu_public_content";
        private String username = "";
        private String password = "";
    }

    @Data
    public static class Sync {
        private String token = "";
        @Min(100)
        private long maxDocuments = 1000000;
    }
}
