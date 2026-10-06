package com.peakui.message.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.message.model.vo.AdminConversationVO;
import com.peakui.message.model.vo.AdminMessageVO;
import com.peakui.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "私信管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/messages/admin")
public class AdminMessageController {

    private final MessageService messageService;

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public ApiResponse<PageResponse<AdminConversationVO>> listConversations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer conversationType,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(messageService.adminListConversations(keyword, conversationType, creatorId, pageNum, pageSize));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<AdminConversationVO> getConversation(@PathVariable Long conversationId) {
        return ApiResponse.success(messageService.adminGetConversation(conversationId));
    }

    @Operation(summary = "消息列表")
    @GetMapping("/messages")
    public ApiResponse<PageResponse<AdminMessageVO>> listMessages(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Integer messageType,
            @RequestParam(required = false) Integer isRecall,
            @RequestParam(required = false) Integer isDeleted,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.success(messageService.adminListMessages(conversationId, senderId, messageType, isRecall, isDeleted,
                keyword, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "撤回消息")
    @PostMapping("/messages/{messageId}/recall")
    public ApiResponse<Void> recallMessage(@PathVariable Long messageId) {
        messageService.adminRecallMessage(messageId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.adminDeleteMessage(messageId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }
}
