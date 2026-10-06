package com.peakui.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import com.peakui.ai.model.ChatCompletionRequest;
import com.peakui.ai.model.ChatMessage;
import com.peakui.ai.model.ChatStreamEvent;
import com.peakui.ai.model.Tool;
import com.peakui.ai.model.ToolCall;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleClient {
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    /** Text-only convenience used by callers that never advertise tools. */
    public Flux<String> stream(List<ChatMessage> messages) {
        return streamEvents(messages, null)
                .ofType(ChatStreamEvent.Token.class)
                .map(ChatStreamEvent.Token::text);
    }

    /** Streams a single completion turn, surfacing any tool calls the model requests. */
    public Flux<ChatStreamEvent> streamEvents(List<ChatMessage> messages, List<Tool> tools) {
        if (!StringUtils.hasText(properties.getModel().getChatUrl())) {
            return Flux.error(new AiBusinessException(503, "AI 模型服务未配置"));
        }
        boolean useTools = tools != null && !tools.isEmpty();
        ChatCompletionRequest request = useTools
                ? new ChatCompletionRequest(properties.getModel().getChatModel(), messages, true,
                        properties.getSecurity().getMaxOutputTokens(), tools, "auto")
                : new ChatCompletionRequest(properties.getModel().getChatModel(), messages, true,
                        properties.getSecurity().getMaxOutputTokens());
        return Flux.defer(() -> {
            AtomicBoolean emitted = new AtomicBoolean();
            AtomicBoolean finished = new AtomicBoolean();
            AtomicReference<String> finishReason = new AtomicReference<>();
            // Keyed by the delta tool_call index; TreeMap keeps call order stable.
            Map<Integer, ToolCallBuilder> builders = new TreeMap<>();
            Flux<ChatStreamEvent> tokens = aiWebClient.post()
                    .uri(properties.getModel().getChatUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> {
                        if (StringUtils.hasText(properties.getModel().getChatApiKey())) {
                            headers.setBearerAuth(properties.getModel().getChatApiKey());
                        }
                    })
                    .bodyValue(request)
                    .retrieve()
                    // SSE 解码器负责分帧，data() 中已经没有 data: 前缀。
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .takeUntil(event -> "[DONE]".equals(event.data()))
                    .handle((event, sink) -> {
                        String data = event.data();
                        if (data == null) return; // heartbeat
                        if ("[DONE]".equals(data)) {
                            finished.set(true);
                            return;
                        }
                        try {
                            JsonNode root = objectMapper.readTree(data);
                            if ("error".equals(event.event()) || root.has("error")) {
                                throw new AiBusinessException(502, "模型服务返回错误，请检查模型权限及额度");
                            }
                            JsonNode choice = root.path("choices").path(0);
                            if (choice.hasNonNull("finish_reason")) {
                                finished.set(true);
                                finishReason.set(choice.path("finish_reason").asText());
                            }
                            JsonNode delta = choice.path("delta");
                            JsonNode content = delta.path("content");
                            // 空格和换行也是模型输出，不能用 hasText 过滤。
                            if (content.isTextual() && !content.asText().isEmpty()) {
                                emitted.set(true);
                                sink.next(new ChatStreamEvent.Token(content.asText()));
                            }
                            accumulateToolCalls(delta.path("tool_calls"), builders);
                        } catch (AiBusinessException e) {
                            sink.error(e);
                        } catch (Exception e) {
                            sink.error(new AiBusinessException(502, "模型流式响应格式错误"));
                        }
                    });
            return tokens
                    .retryWhen(Retry.backoff(properties.getModel().getMaxRetries(), Duration.ofMillis(200))
                            .filter(error -> !emitted.get() && builders.isEmpty() && retryable(error))
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                    .onErrorMap(this::sanitizeUpstreamError)
                    .concatWith(Mono.defer(() -> {
                        List<ToolCall> calls = buildToolCalls(builders);
                        // A tool-call-only turn carries no text, so success means
                        // "text or tool calls", not "text".
                        if (calls.isEmpty() && !emitted.get()) {
                            return Mono.error(new AiBusinessException(502, "模型未返回文字"));
                        }
                        if (!finished.get()) {
                            return Mono.error(new AiBusinessException(502, "模型输出意外中断"));
                        }
                        return Mono.just(new ChatStreamEvent.Completed(calls, finishReason.get()));
                    }));
        });
    }

    private static void accumulateToolCalls(JsonNode calls, Map<Integer, ToolCallBuilder> builders) {
        if (!calls.isArray()) return;
        for (JsonNode call : calls) {
            ToolCallBuilder builder = builders.computeIfAbsent(call.path("index").asInt(0), ignored -> new ToolCallBuilder());
            if (call.hasNonNull("id")) builder.id = call.path("id").asText();
            if (call.hasNonNull("type")) builder.type = call.path("type").asText();
            JsonNode function = call.path("function");
            if (function.hasNonNull("name")) builder.name = function.path("name").asText();
            if (function.hasNonNull("arguments")) builder.arguments.append(function.path("arguments").asText());
        }
    }

    private static List<ToolCall> buildToolCalls(Map<Integer, ToolCallBuilder> builders) {
        if (builders.isEmpty()) return List.of();
        List<ToolCall> calls = new ArrayList<>(builders.size());
        for (ToolCallBuilder builder : builders.values()) {
            if (!StringUtils.hasText(builder.name)) continue;
            calls.add(new ToolCall(builder.id, builder.type,
                    new ToolCall.FunctionCall(builder.name, builder.arguments.toString())));
        }
        return calls;
    }

    private Throwable sanitizeUpstreamError(Throwable error) {
        if (error instanceof AiBusinessException) return error;
        if (error instanceof WebClientResponseException) {
            return new AiBusinessException(502, "模型服务响应异常");
        }
        return new AiBusinessException(503, "模型服务暂时不可用，请稍后重试");
    }

    private boolean retryable(Throwable error) {
        if (error instanceof WebClientResponseException response) {
            return response.getStatusCode().value() == 429 || response.getStatusCode().is5xxServerError();
        }
        return error instanceof org.springframework.web.reactive.function.client.WebClientRequestException;
    }

    private static final class ToolCallBuilder {
        private String id;
        private String type = "function";
        private String name;
        private final StringBuilder arguments = new StringBuilder();
    }
}
