package com.peakui.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.AiProperties;
import com.peakui.ai.exception.AiBusinessException;
import com.peakui.ai.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatibleClientTest {
    private AiProperties properties;
    private static final List<ChatMessage> MESSAGES = List.of(new ChatMessage("user", "你好"));
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.getModel().setChatUrl("https://model.invalid/v1/chat/completions");
        properties.getModel().setChatModel("test-model");
        properties.getModel().setChatApiKey("test-key");
        properties.getModel().setMaxRetries(0);
    }

    private OpenAiCompatibleClient client(ExchangeFunction exchange) {
        return new OpenAiCompatibleClient(WebClient.builder().exchangeFunction(exchange).build(),
                new ObjectMapper(), properties);
    }

    private ClientResponse sse(String body) {
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                .body(body).build();
    }

    private String token(String content) throws Exception {
        return "data: {\"choices\":[{\"delta\":{\"content\":"
                + new ObjectMapper().writeValueAsString(content) + "}}]}\n\n";
    }

    @Test
    void decodesSseAndPreservesWhitespace() throws Exception {
        String body = ": heartbeat\n\n" + token("你好") + token(" ") + token("\n") + "data: [DONE]\n\n";
        OpenAiCompatibleClient client = client(request -> {
            assertEquals("Bearer test-key", request.headers().getFirst("Authorization"));
            assertEquals(MediaType.APPLICATION_JSON, request.headers().getContentType());
            assertTrue(request.headers().getAccept().contains(MediaType.TEXT_EVENT_STREAM));
            return Mono.just(sse(body));
        });
        assertEquals(List.of("你好", " ", "\n"), client.stream(MESSAGES).collectList().block(TIMEOUT));
    }

    @Test
    void decodesUtf8AndFramesSplitAcrossByteBoundaries() throws Exception {
        byte[] bytes = (token("中文") + "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
        ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                .body(Flux.range(0, bytes.length).map(i -> DefaultDataBufferFactory.sharedInstance.wrap(new byte[]{bytes[i]})))
                .build();
        assertEquals(List.of("中文"), client(request -> Mono.just(response)).stream(MESSAGES).collectList().block(TIMEOUT));
    }

    @Test
    void acceptsFinishReasonWithoutDoneMarker() throws Exception {
        String body = token("回答") + "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n";
        assertEquals(List.of("回答"), client(request -> Mono.just(sse(body))).stream(MESSAGES).collectList().block(TIMEOUT));
    }

    @Test
    void rejectsEmptyCompletion() {
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> Mono.just(sse("data: [DONE]\n\n"))).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals("模型未返回文字", error.getMessage());
    }

    @Test
    void rejectsTruncatedCompletion() throws Exception {
        String body = token("部分回答");
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> Mono.just(sse(body))).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals("模型输出意外中断", error.getMessage());
    }

    @Test
    void hidesUpstreamErrorPayload() {
        AiBusinessException error = assertThrows(AiBusinessException.class, () -> client(request -> Mono.just(
                sse("event: error\ndata: {\"error\":{\"message\":\"private upstream details\"}}\n\n")))
                .stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals("模型服务返回错误，请检查模型权限及额度", error.getMessage());
    }

    @Test
    void rejectsMalformedJson() {
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> Mono.just(sse("data: {invalid}\n\n"))).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals("模型流式响应格式错误", error.getMessage());
    }

    @Test
    void retriesServerFailureBeforeFirstToken() throws Exception {
        properties.getModel().setMaxRetries(1);
        AtomicInteger calls = new AtomicInteger();
        String body = token("成功") + "data: [DONE]\n\n";
        OpenAiCompatibleClient client = client(request -> Mono.fromSupplier(() -> calls.incrementAndGet() == 1
                ? ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build() : sse(body)));
        assertEquals(List.of("成功"), client.stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals(2, calls.get());
    }

    @Test
    void doesNotRetryAfterOutputHasStarted() throws Exception {
        properties.getModel().setMaxRetries(2);
        AtomicInteger calls = new AtomicInteger();
        byte[] firstToken = token("已经输出").getBytes(StandardCharsets.UTF_8);
        WebClientResponseException failure = WebClientResponseException.create(503, "Unavailable", null, null, null);
        OpenAiCompatibleClient client = client(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                    .body(Flux.concat(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(firstToken)), Flux.error(failure)))
                    .build());
        });
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client.stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals(502, error.getStatus());
        assertEquals("模型服务响应异常", error.getMessage());
        assertFalse(error.getMessage().contains("Unavailable"));
        assertEquals(1, calls.get());
    }

    @Test
    void mapsExhaustedUpstreamResponseToSafe502() {
        properties.getModel().setMaxRetries(0);
        ClientResponse response = ClientResponse.create(HttpStatus.BAD_GATEWAY)
                .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                .body("private upstream details").build();
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> Mono.just(response)).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals(502, error.getStatus());
        assertEquals("模型服务响应异常", error.getMessage());
        assertFalse(error.getMessage().contains("private upstream details"));
    }

    @Test
    void mapsUnexpectedUpstreamFailureToSafe503() {
        RuntimeException failure = new RuntimeException("private upstream details");
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> Mono.error(failure)).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals(503, error.getStatus());
        assertEquals("模型服务暂时不可用，请稍后重试", error.getMessage());
        assertFalse(error.getMessage().contains("private upstream details"));
    }

    @Test
    void rejectsMissingModelUrlWithoutMakingRequest() {
        properties.getModel().setChatUrl(null);
        AiBusinessException error = assertThrows(AiBusinessException.class,
                () -> client(request -> { throw new AssertionError("不应发送请求"); }).stream(MESSAGES).collectList().block(TIMEOUT));
        assertEquals("AI 模型服务未配置", error.getMessage());
    }
}
