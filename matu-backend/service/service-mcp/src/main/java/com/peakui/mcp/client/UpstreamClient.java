package com.peakui.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.mcp.McpProperties;
import com.peakui.mcp.McpToolException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the read-only APIs of other services through Nacos discovery, mirroring
 * {@code com.peakui.ai.mq.PostAiCallbackClient}. The response data is kept as a
 * raw {@link JsonNode} so tools never duplicate the downstream DTOs.
 */
@Component
@RequiredArgsConstructor
public class UpstreamClient {

    private final DiscoveryClient discovery;
    private final ObjectMapper objectMapper;
    private final McpProperties properties;

    public JsonNode getJson(String serviceName, String path, Map<String, Object> query) {
        List<ServiceInstance> instances = discovery.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new McpToolException("服务不可用: " + serviceName);
        }
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(properties.getDownstreamTimeoutMs(), 5000))).build());
        factory.setReadTimeout(Duration.ofMillis(properties.getDownstreamTimeoutMs()));

        ApiResponse<JsonNode> response;
        try {
            response = RestClient.builder()
                    .baseUrl(instances.get(0).getUri().toString())
                    .requestFactory(factory)
                    .build()
                    .get()
                    .uri(builder -> {
                        builder.path(path);
                        query.forEach((key, value) -> {
                            if (value != null) {
                                builder.queryParam(key, value);
                            }
                        });
                        return builder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RuntimeException e) {
            throw new McpToolException(serviceName + " 调用失败: " + e.getMessage());
        }
        if (response == null || response.getCode() == null || response.getCode() != 0) {
            String message = response == null ? "无响应" : response.getMessage();
            throw new McpToolException(serviceName + " 返回错误: " + message);
        }
        return response.getData() == null ? objectMapper.nullNode() : response.getData();
    }
}
