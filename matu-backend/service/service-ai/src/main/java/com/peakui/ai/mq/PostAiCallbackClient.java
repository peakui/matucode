package com.peakui.ai.mq;

import com.peakui.ai.AiProperties;
import com.peakui.ai.model.ApplyAiSummaryCommentRequest;
import com.peakui.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PostAiCallbackClient {
    private final AiProperties properties;
    private final DiscoveryClient discovery;

    public void apply(Long postId, String summary, String comment, String sourceHash) {
        String serviceUrl = properties.getPost().getServiceUrl();
        String token = properties.getPost().getInternalToken();
        if (serviceUrl == null || serviceUrl.isBlank() || token == null || token.isBlank()) {
            throw new IllegalStateException("AI 回调地址或服务凭证未配置");
        }
        if ("http://service-post".equals(serviceUrl)) {
            var instances = discovery.getInstances("service-post");
            if (instances.isEmpty()) throw new IllegalStateException("Nacos中没有可用的service-post实例");
            serviceUrl = instances.get(0).getUri().toString();
        }
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        ApiResponse<Void> response = RestClient.builder().baseUrl(serviceUrl).requestFactory(factory).build()
                .post().uri("/posts/internal/ai/{postId}/summary-comment", postId)
                .header("X-AI-Service-Token", token)
                .body(new ApplyAiSummaryCommentRequest(summary, comment, sourceHash))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getCode() == null || response.getCode() != 0) {
            throw new IllegalStateException("service-post 拒绝保存 AI 评论");
        }
    }
}
