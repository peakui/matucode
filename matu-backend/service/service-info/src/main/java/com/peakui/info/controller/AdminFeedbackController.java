package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.UpdateFeedbackRequest;
import com.peakui.info.model.vo.FeedbackDetailVO;
import com.peakui.info.model.vo.FeedbackListItemVO;
import com.peakui.info.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "反馈管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/info/feedbacks")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "反馈列表")
    @GetMapping
    public ApiResponse<PageResponse<FeedbackListItemVO>> listFeedbacks(@RequestParam(required = false) Integer type,
                                                                       @RequestParam(required = false) Integer status,
                                                                       @RequestParam(required = false) Integer priority,
                                                                       @RequestParam(required = false) Long assigneeId,
                                                                       @RequestParam(required = false) Long userId,
                                                                       @RequestParam(required = false) String keyword,
                                                                       @RequestParam(defaultValue = "1") Long pageNum,
                                                                       @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(feedbackService.listAdminFeedbacks(type, status, priority, assigneeId, userId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "反馈详情")
    @GetMapping("/{id}")
    public ApiResponse<FeedbackDetailVO> getFeedbackDetail(@PathVariable Long id) {
        return ApiResponse.success(feedbackService.getAdminFeedbackDetail(id));
    }

    @Operation(summary = "更新反馈")
    @PutMapping("/{id}")
    public ApiResponse<FeedbackDetailVO> updateFeedback(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateFeedbackRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), feedbackService.updateFeedback(id, request));
    }
}
