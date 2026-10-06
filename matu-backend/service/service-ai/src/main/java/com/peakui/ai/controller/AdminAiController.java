package com.peakui.ai.controller;

import com.peakui.ai.AiProperties;
import com.peakui.ai.agent.AiAgentService;
import com.peakui.ai.exception.AiBusinessException;
import com.peakui.ai.mcp.McpClient;
import com.peakui.ai.model.AdminAiConversationVO;
import com.peakui.ai.model.AdminAiMessageVO;
import com.peakui.ai.model.AiAdminStatusVO;
import com.peakui.ai.model.McpServerVO;
import com.peakui.ai.record.AiRecordStore;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/admin")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiRecordStore recordStore;
    private final AiAgentService agentService;
    private final AiProperties properties;
    private final StringRedisTemplate redis;

    @GetMapping("/status")
    public ApiResponse<AiAdminStatusVO> status() {
        return ApiResponse.success(new AiAdminStatusVO(
                recordStore.isAvailable(),
                properties.getMcp().isEnabled(),
                properties.getMcp().getServers().size(),
                properties.getRag().isEnabled(),
                properties.getModel().getChatModel(),
                properties.getModel().getEmbeddingModel()));
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResponse<AdminAiConversationVO>> conversations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(recordStore.listAllConversations(keyword, pageNum, pageSize));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<AdminAiMessageVO>> conversationMessages(
            @PathVariable String conversationId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "50") long pageSize) {
        return ApiResponse.success(recordStore.listAllMessages(conversationId, userId, pageNum, pageSize));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<Object, Object>> task(@PathVariable String taskId) {
        Map<Object, Object> task = redis.opsForHash().entries("ai:task:" + taskId);
        if (task.isEmpty()) {
            throw new AiBusinessException(404, "任务不存在");
        }
        return ApiResponse.success(task);
    }

    @GetMapping("/mcp/servers")
    public ApiResponse<List<McpServerVO>> mcpServers() {
        List<McpServerVO> servers = properties.getMcp().getServers().stream()
                .map(server -> new McpServerVO(
                        server.getName(),
                        server.getUrl(),
                        server.isEnabled(),
                        server.getAllowTools(),
                        server.getAllowTools() == null ? 0 : server.getAllowTools().size()))
                .toList();
        return ApiResponse.success(servers);
    }

    @GetMapping("/mcp/tools")
    public ApiResponse<List<McpClient.ToolDescriptor>> mcpTools() {
        return ApiResponse.success(agentService.tools());
    }
}
