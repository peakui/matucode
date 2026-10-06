package com.peakui.message.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.message.model.dto.CreateGroupConversationRequest;
import com.peakui.message.model.dto.CreateSingleConversationRequest;
import com.peakui.message.model.dto.MarkConversationReadRequest;
import com.peakui.message.model.dto.MarkNotificationsReadRequest;
import com.peakui.message.model.dto.SendMessageRequest;
import com.peakui.message.model.vo.ConversationVO;
import com.peakui.message.model.vo.MessageVO;
import com.peakui.message.model.vo.NotificationVO;
import com.peakui.message.service.MessageService;
import com.peakui.message.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "私信接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final NotificationService notificationService;

    @Operation(summary = "创建或复用单聊会话")
    @PostMapping("/conversations/single")
    public ApiResponse<ConversationVO> createSingleConversation(@Valid @RequestBody CreateSingleConversationRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), messageService.createSingleConversation(request));
    }

    @Operation(summary = "创建群聊会话")
    @PostMapping("/conversations/group")
    public ApiResponse<ConversationVO> createGroupConversation(@Valid @RequestBody CreateGroupConversationRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), messageService.createGroupConversation(request));
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("/conversations")
    public ApiResponse<PageResponse<ConversationVO>> listConversations(@RequestParam(defaultValue = "1") Long pageNum,
                                                                       @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(messageService.listConversations(pageNum, pageSize));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationVO> getConversation(@PathVariable Long conversationId) {
        return ApiResponse.success(messageService.getConversation(conversationId));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<MessageVO> sendMessage(@PathVariable Long conversationId, @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), messageService.sendMessage(conversationId, request));
    }

    @Operation(summary = "消息列表")
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<MessageVO>> listMessages(@PathVariable Long conversationId,
                                                            @RequestParam(defaultValue = "1") Long pageNum,
                                                            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.success(messageService.listMessages(conversationId, pageNum, pageSize));
    }

    @Operation(summary = "标记会话已读")
    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> markConversationRead(@PathVariable Long conversationId, @RequestBody MarkConversationReadRequest request) {
        messageService.markConversationRead(conversationId, request == null ? new MarkConversationReadRequest() : request);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "置顶会话")
    @PostMapping("/conversations/{conversationId}/top")
    public ApiResponse<Void> topConversation(@PathVariable Long conversationId) {
        messageService.topConversation(conversationId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "取消置顶会话")
    @DeleteMapping("/conversations/{conversationId}/top")
    public ApiResponse<Void> untopConversation(@PathVariable Long conversationId) {
        messageService.untopConversation(conversationId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "会话免打扰")
    @PostMapping("/conversations/{conversationId}/mute")
    public ApiResponse<Void> muteConversation(@PathVariable Long conversationId) {
        messageService.muteConversation(conversationId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "取消会话免打扰")
    @DeleteMapping("/conversations/{conversationId}/mute")
    public ApiResponse<Void> unmuteConversation(@PathVariable Long conversationId) {
        messageService.unmuteConversation(conversationId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "删除自己发送的消息")
    @DeleteMapping("/{messageId:\\d+}")
    public ApiResponse<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "撤回自己发送的消息")
    @PostMapping("/{messageId:\\d+}/recall")
    public ApiResponse<Void> recallMessage(@PathVariable Long messageId) {
        messageService.recallMessage(messageId);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }

    @Operation(summary = "我的通知列表（评论回复）")
    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NotificationVO>> listNotifications(@RequestParam(defaultValue = "1") Long pageNum,
                                                                       @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.success(notificationService.listNotifications(pageNum, pageSize));
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> unreadNotificationCount() {
        return ApiResponse.success(notificationService.unreadCount());
    }

    @Operation(summary = "标记通知已读")
    @PostMapping("/notifications/read")
    public ApiResponse<Void> markNotificationsRead(@RequestBody(required = false) MarkNotificationsReadRequest request) {
        notificationService.markRead(request);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }
}
