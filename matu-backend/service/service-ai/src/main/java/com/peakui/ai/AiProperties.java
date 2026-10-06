package com.peakui.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private Model model = new Model();
    private Security security = new Security();
    private Agent agent = new Agent();
    private Rag rag = new Rag();
    private A2a a2a = new A2a();
    private Mcp mcp = new Mcp();
    private Post post = new Post();
    private RecordProperties record = new RecordProperties();

    @Data
    public static class Model {
        private String chatUrl;
        private String embeddingUrl;
        private String chatApiKey;
        private String embeddingApiKey;
        private String chatModel;
        private String embeddingModel;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 120000;
        private int maxRetries = 2;
    }

    @Data
    public static class Security {
        private int rateLimitPerMinute = 20;
        private int concurrentLimit = 2;
        private int maxPromptChars = 12000;
        private int maxOutputTokens = 2000;
    }

    @Data
    public static class Agent {
        private int maxSteps = 6;
        private int maxToolCalls = 4;
        private int taskTtlSeconds = 3600;
        /** When false the agent skips the function-calling loop entirely. */
        private boolean toolLoopEnabled = true;
        /** Hard cap on characters of a single tool result fed back to the model. */
        private int maxToolResultChars = 8000;
    }

    @Data
    public static class Rag {
        private boolean enabled = true;
        private int topK = 5;
        private double minScore = 0.70;
        private int embeddingDimension = 1536;
        private boolean autoInit = false;
    }

    @Data
    public static class A2a {
        private boolean enabled = true;
        private String agentName = "matu-ai";
    }

    @Data
    public static class Post {
        private boolean automationEnabled = true;
        private String serviceUrl = "http://service-post";
        private String internalToken;
        private long commentUserId = 900000000000000001L;
    }

    /**
     * Durable AI chat records in local MySQL. Deliberately separate from the
     * primary PostgreSQL datasource (pgvector RAG); see AiRecordStore for why this
     * one is not exposed as a Spring DataSource bean.
     */
    @Data
    public static class RecordProperties {
        private boolean enabled = true;
        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private int maximumPoolSize = 5;
        private int minimumIdle = 1;
        private long connectionTimeoutMs = 5000;
    }

    @Data
    public static class Mcp {        private boolean enabled = false;
        private int requestTimeoutMs = 10000;
        private List<Server> servers = new ArrayList<>();
    }

    @Data
    public static class Server {
        private String name;
        private String url;
        private String apiKey;
        private boolean enabled = true;
        private List<String> allowTools = new ArrayList<>();
    }
}
