package com.peakui.ai.a2a;

import com.peakui.ai.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class A2aController {
    private final AiProperties properties;

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "name", properties.getA2a().getAgentName(),
                "description", "Matu 平台安全 AI 智能体，支持 SSE 对话、记忆、RAG、MCP 工具和异步任务",
                "protocolVersion", "a2a-compatible-boundary-v1",
                "capabilities", Map.of("streaming", true, "pushNotifications", false),
                "skills", List.of("chat", "rag-search", "mcp-tool-routing", "async-task"),
                "endpoints", Map.of("stream", "/ai/chat/stream", "async", "/ai/chat/async")
        );
    }
}
