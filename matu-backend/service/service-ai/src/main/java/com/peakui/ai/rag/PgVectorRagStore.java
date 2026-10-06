package com.peakui.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.AiProperties;
import com.peakui.ai.model.RagDocumentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PgVectorRagStore {
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final AiProperties properties;

    @PostConstruct
    public void prepareSchema() {
        initialize();
    }

    public void initialize() {
        if (!properties.getRag().isAutoInit()) {
            return;
        }
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ai_knowledge (" +
                "id BIGSERIAL PRIMARY KEY, tenant_id VARCHAR(128) NOT NULL, title TEXT, " +
                "content TEXT NOT NULL, embedding vector(" + properties.getRag().getEmbeddingDimension() + "), " +
                "created_at TIMESTAMPTZ NOT NULL DEFAULT now())");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS ai_knowledge_tenant_idx ON ai_knowledge(tenant_id)");
    }

    public void save(RagDocumentRequest request) {
        String tenant = StringUtils.hasText(request.tenantId()) ? request.tenantId() : "default";
        float[] vector = embeddingClient.embed(request.content());
        String literal = vectorLiteral(vector);
        jdbcTemplate.update("INSERT INTO ai_knowledge(tenant_id,title,content,embedding) VALUES (?,?,?,?::vector)",
                tenant, request.title(), request.content(), literal);
    }

    public List<Hit> search(String tenantId, String query) {
        if (!properties.getRag().isEnabled() || !StringUtils.hasText(properties.getModel().getEmbeddingUrl())) {
            return List.of();
        }
        float[] vector = embeddingClient.embed(query);
        String literal = vectorLiteral(vector);
        String tenant = StringUtils.hasText(tenantId) ? tenantId : "default";
        return jdbcTemplate.query("SELECT title,content,1 - (embedding <=> ?::vector) AS score " +
                        "FROM ai_knowledge WHERE tenant_id=? AND embedding IS NOT NULL " +
                        "ORDER BY embedding <=> ?::vector LIMIT ?",
                (rs, rowNum) -> new Hit(rs.getString("title"), rs.getString("content"), rs.getDouble("score")),
                literal, tenant, literal, properties.getRag().getTopK());
    }

    private String vectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(vector[i]);
        }
        return builder.append(']').toString();
    }

    public record Hit(String title, String content, double score) {
    }
}
