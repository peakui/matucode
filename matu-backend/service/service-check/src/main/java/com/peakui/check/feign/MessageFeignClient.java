package com.peakui.check.feign;

import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-message", path = "/messages/internal")
public interface MessageFeignClient {

    @PostMapping("/notifications")
    ApiResponse<Void> notifyCommentReply(@RequestBody CommentReplyNotifyRequest request);
}
