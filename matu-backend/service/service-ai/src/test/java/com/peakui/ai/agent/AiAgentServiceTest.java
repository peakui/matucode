package com.peakui.ai.agent;

import com.peakui.ai.AiProperties;
import com.peakui.ai.llm.OpenAiCompatibleClient;
import com.peakui.ai.memory.ConversationMemory;
import com.peakui.ai.mcp.McpClient;
import com.peakui.ai.model.ChatRequest;
import com.peakui.ai.rag.PgVectorRagStore;
import com.peakui.ai.record.AiRecordStore;
import com.peakui.ai.security.AiRateLimiter;
import com.peakui.ai.security.PromptGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AiAgentServiceTest {
    @Test
    void releasesLeaseWhenMemoryPreprocessingFails() {
        PromptGuard promptGuard = mock(PromptGuard.class);
        AiRateLimiter rateLimiter = mock(AiRateLimiter.class);
        ConversationMemory memory = mock(ConversationMemory.class);
        AiRecordStore recordStore = mock(AiRecordStore.class);
        PgVectorRagStore ragStore = mock(PgVectorRagStore.class);
        McpClient mcpClient = mock(McpClient.class);
        OpenAiCompatibleClient llm = mock(OpenAiCompatibleClient.class);
        AiProperties properties = new AiProperties();
        AiRateLimiter.Lease lease = new AiRateLimiter.Lease("active:user", "token");

        when(promptGuard.checkAndNormalize("hello")).thenReturn("hello");
        when(rateLimiter.acquire("user")).thenReturn(lease);
        when(memory.load("user", null)).thenThrow(new IllegalStateException("memory unavailable"));

        AiAgentService service = new AiAgentService(
                promptGuard, rateLimiter, memory, recordStore, ragStore, mcpClient, llm, properties,
                new com.fasterxml.jackson.databind.ObjectMapper());

        assertThrows(IllegalStateException.class,
                () -> service.streamAsUser("user", new ChatRequest(null, "hello", true, null)));
        verify(rateLimiter).release(lease);
        verifyNoInteractions(llm);
    }
}
