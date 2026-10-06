package com.peakui.ai.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.ai.AiProperties;
import com.peakui.ai.llm.OpenAiCompatibleClient;
import com.peakui.common.security.PostContentFingerprint;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostAiAutomationServiceTest {
    private final OpenAiCompatibleClient llm = mock(OpenAiCompatibleClient.class);
    private final PostAiCallbackClient callback = mock(PostAiCallbackClient.class);
    private final AiProperties properties = new AiProperties();
    private final PostAiAutomationService service = new PostAiAutomationService(llm, callback, properties, new ObjectMapper());

    @Test void summaryIsPostedWithSourceFingerprint() {
        when(llm.stream(anyList())).thenReturn(Flux.just("{\"summary\":", "\"这是一篇技术文章\"}"));
        service.process(42L, "标题", "正文");
        verify(callback).apply(eq(42L), eq("这是一篇技术文章"), contains("AI 自动生成"),
                eq(PostContentFingerprint.of("标题", "正文")));
    }

    @Test void invalidOutputNeverBecomesPublicComment() {
        when(llm.stream(anyList())).thenReturn(Flux.just("模型失败：原始响应"));
        assertThrows(IllegalStateException.class, () -> service.process(42L, "标题", "正文"));
        verifyNoInteractions(callback);
    }

    @Test void emptyOrTruncatedStreamNeverPosts() {
        when(llm.stream(anyList())).thenReturn(Flux.error(new IllegalStateException("interrupted")));
        assertThrows(IllegalStateException.class, () -> service.process(42L, "标题", "正文"));
        verifyNoInteractions(callback);
    }

    @Test void disabledAutomationDoesNotSilentlyAcknowledge() {
        properties.getPost().setAutomationEnabled(false);
        assertThrows(IllegalStateException.class, () -> service.process(42L, "标题", "正文"));
        verifyNoInteractions(llm, callback);
    }

    @Test void strictParsingAndBoundedLength() {
        assertThrows(IllegalStateException.class, () -> service.parseSummary("{}"));
        assertThrows(IllegalStateException.class, () -> service.parseSummary("{\"summary\":\"ok\"} {}"));
        assertThrows(IllegalStateException.class, () -> service.parseSummary("{\"summary\":\"<p></p>\"}"));
        assertEquals("内容", service.parseSummary("```json\n{\"summary\":\"内容\"}\n```"));
        assertEquals(500, service.parseSummary("{\"summary\":\"" + "中".repeat(800) + "\"}").length());
    }
}
