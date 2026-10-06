package com.peakui.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.ai.agent.AiAgentService;
import com.peakui.ai.exception.AiBusinessException;
import com.peakui.ai.mcp.McpClient;
import com.peakui.ai.model.AiConversationVO;
import com.peakui.ai.model.AiMessageVO;
import com.peakui.ai.model.AsyncTaskResponse;
import com.peakui.ai.model.ChatRequest;
import com.peakui.ai.model.RagDocumentRequest;
import com.peakui.ai.model.RenameConversationRequest;
import com.peakui.ai.mq.AiTaskProducer;
import com.peakui.ai.rag.PgVectorRagStore;
import com.peakui.ai.record.AiRecordStore;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiAgentService agentService;
    private final AiTaskProducer taskProducer;
    private final StringRedisTemplate redis;
    private final PgVectorRagStore ragStore;
    private final AiRecordStore recordStore;
    private final McpClient mcpClient;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
        StpUtil.checkLogin();
        return agentService.stream(request)
                .map(token -> ServerSentEvent.<String>builder().event("token").data(token).build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build()))
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder().event("error")
                        .data(error.getMessage() == null ? "AI 请求失败" : error.getMessage()).build()));
    }

    @PostMapping("/chat/async")
    public AsyncTaskResponse async(@Valid @RequestBody ChatRequest request) {
        StpUtil.checkLogin();
        String taskId = UUID.randomUUID().toString();
        String userId = StpUtil.getLoginIdAsString();
        redis.opsForHash().put("ai:task:" + taskId, "status", "QUEUED");
        redis.opsForHash().put("ai:task:" + taskId, "userId", userId);
        redis.expire("ai:task:" + taskId, Duration.ofHours(1));
        taskProducer.send(taskId, userId, request);
        return new AsyncTaskResponse(taskId, "QUEUED");
    }

    @GetMapping("/tasks/{taskId}")
    public Map<Object, Object> task(@PathVariable String taskId) {
        StpUtil.checkLogin();
        Map<Object, Object> task = redis.opsForHash().entries("ai:task:" + taskId);
        if (task.isEmpty() || !StpUtil.getLoginIdAsString().equals(String.valueOf(task.get("userId")))) {
            throw new AiBusinessException(404, "任务不存在");
        }
        return task;
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResponse<AiConversationVO>> conversations(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(defaultValue = "1") long pageNum,
                                                                     @RequestParam(defaultValue = "20") long pageSize) {
        StpUtil.checkLogin();
        return ApiResponse.success(recordStore.listConversations(StpUtil.getLoginIdAsString(), keyword, pageNum, pageSize));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<Boolean> deleteConversation(@PathVariable String conversationId) {
        StpUtil.checkLogin();
        String userId = StpUtil.getLoginIdAsString();
        if (!recordStore.conversationExists(userId, conversationId)) {
            throw new AiBusinessException(404, "会话不存在");
        }
        return ApiResponse.success(recordStore.deleteConversation(userId, conversationId));
    }

    @PutMapping("/conversations/{conversationId}")
    public ApiResponse<Boolean> renameConversation(@PathVariable String conversationId,
                                                   @Valid @RequestBody RenameConversationRequest request) {
        StpUtil.checkLogin();
        String userId = StpUtil.getLoginIdAsString();
        if (!recordStore.conversationExists(userId, conversationId)) {
            throw new AiBusinessException(404, "会话不存在");
        }
        return ApiResponse.success(recordStore.renameConversation(userId, conversationId, request.title()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<AiMessageVO>> conversationMessages(@PathVariable String conversationId,
                                                                      @RequestParam(defaultValue = "1") long pageNum,
                                                                      @RequestParam(defaultValue = "50") long pageSize) {
        StpUtil.checkLogin();
        String userId = StpUtil.getLoginIdAsString();
        if (!recordStore.conversationExists(userId, conversationId)) {
            throw new AiBusinessException(404, "会话不存在");
        }
        return ApiResponse.success(recordStore.listMessages(userId, conversationId, pageNum, pageSize));
    }

    @GetMapping("/mcp/tools")
    public java.util.List<McpClient.ToolDescriptor> tools() {        StpUtil.checkLogin();
        return agentService.tools();
    }

    @PostMapping("/mcp/{server}/{tool}")
    public Object callTool(@PathVariable String server,
                           @PathVariable String tool,
                           @RequestBody(required = false) Map<String, Object> arguments) {
        StpUtil.checkLogin();
        return mcpClient.call(server, tool, arguments == null ? Map.of() : arguments);
    }

    @PostMapping("/rag/documents")
    public Map<String, String> addDocument(@Valid @RequestBody RagDocumentRequest request) {
        StpUtil.checkLogin();
        ragStore.save(request);
        return Map.of("status", "INDEXED");
    }
}
