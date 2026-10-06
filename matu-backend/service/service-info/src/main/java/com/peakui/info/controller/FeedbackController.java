package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateFeedbackRequest;
import com.peakui.info.model.vo.FeedbackDetailVO;
import com.peakui.info.model.vo.FeedbackListItemVO;
import com.peakui.info.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户反馈接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈")
    @PostMapping
    public ApiResponse<FeedbackDetailVO> createFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), feedbackService.createFeedback(request));
    }

    @Operation(summary = "我的反馈列表")
    @GetMapping("/mine")
    public ApiResponse<PageResponse<FeedbackListItemVO>> listMyFeedbacks(@RequestParam(defaultValue = "1") Long pageNum,
                                                                        @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(feedbackService.listMyFeedbacks(pageNum, pageSize));
    }

    @Operation(summary = "反馈详情")
    @GetMapping("/{id}")
    public ApiResponse<FeedbackDetailVO> getFeedbackDetail(@PathVariable Long id) {
        return ApiResponse.success(feedbackService.getFeedbackDetail(id));
    }
}
