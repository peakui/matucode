package com.peakui.pay.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.pay.model.dto.RefundRequest;
import com.peakui.pay.model.vo.RefundVO;
import com.peakui.pay.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "退款")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay/refunds")
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "发起退款")
    @PostMapping
    public ApiResponse<RefundVO> refund(@Valid @RequestBody RefundRequest request) {
        return ApiResponse.success(refundService.refund(request));
    }
}
