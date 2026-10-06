package com.peakui.message.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.common.result.ApiResponse;
import com.peakui.message.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间内部接口：内容服务（post/check/qa）评论成功后调用，落库并推送 WebSocket。
 * 通过服务名直连，不经网关，故不依赖调用方登录态。
 */
@Tag(name = "通知内部接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/messages/internal")
public class NotificationInternalController {

    private final NotificationService notificationService;

    @Operation(summary = "评论回复通知")
    @PostMapping("/notifications")
    public ApiResponse<Void> notifyCommentReply(@RequestBody CommentReplyNotifyRequest request) {
        notificationService.notifyCommentReply(request);
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), null);
    }
}
