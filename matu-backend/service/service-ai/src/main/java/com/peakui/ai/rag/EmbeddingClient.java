package com.peakui.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class EmbeddingClient {
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public float[] embed(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getModel().getEmbeddingModel());
            ArrayNode input = body.putArray("input");
            input.add(text);
            JsonNode root = aiWebClient.post()
                    .uri(properties.getModel().getEmbeddingUrl())
                    .headers(headers -> {
                        if (StringUtils.hasText(properties.getModel().getEmbeddingApiKey())) {
                            headers.setBearerAuth(properties.getModel().getEmbeddingApiKey());
                        }
                    })
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            // DashScope (Aliyun) native shape: output.embeddings[0].embedding.
            JsonNode values = root == null ? null : root.path("output").path("embeddings").path(0).path("embedding");
            if (values == null || !values.isArray()) {
                // OpenAI-compatible shape: data[0].embedding.
                values = root == null ? null : root.path("data").path(0).path("embedding");
            }
            if (values == null || !values.isArray()) {
                throw new AiBusinessException(502, "向量模型返回格式错误");
            }
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) result[i] = (float) values.get(i).asDouble();
            return result;
        } catch (AiBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new AiBusinessException(502, "向量模型暂时不可用");
        }
    }
}
