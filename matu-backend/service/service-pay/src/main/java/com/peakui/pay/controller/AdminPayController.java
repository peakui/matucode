package com.peakui.pay.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.pay.model.dto.AdminRefundRequest;
import com.peakui.pay.model.vo.AdminOrderVO;
import com.peakui.pay.model.vo.AdminRefundVO;
import com.peakui.pay.model.vo.AdminTransactionVO;
import com.peakui.pay.model.vo.RefundVO;
import com.peakui.pay.service.AdminPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@Tag(name = "支付管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay/admin")
public class AdminPayController {

    private final AdminPayService adminPayService;

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public ApiResponse<PageResponse<AdminOrderVO>> listOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(adminPayService.listOrders(orderNo, userId, status, productName, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<AdminOrderVO> getOrder(@PathVariable String orderNo) {
        return ApiResponse.success(adminPayService.getOrder(orderNo));
    }

    @Operation(summary = "支付流水列表")
    @GetMapping("/transactions")
    public ApiResponse<PageResponse<AdminTransactionVO>> listTransactions(
            @RequestParam(required = false) String transactionNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(adminPayService.listTransactions(transactionNo, orderNo, channel, status, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "退款列表")
    @GetMapping("/refunds")
    public ApiResponse<PageResponse<AdminRefundVO>> listRefunds(
            @RequestParam(required = false) String refundNo,
            @RequestParam(required = false) String transactionNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(adminPayService.listRefunds(refundNo, transactionNo, orderNo, status, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "发起退款")
    @PostMapping("/refunds")
    public ApiResponse<RefundVO> createRefund(@Valid @RequestBody AdminRefundRequest request) {
        // Refunds hit the real Alipay account and have no idempotency key: record who asked for what.
        log.warn("admin refund requested: operator={}, transactionNo={}, amount={}, reason={}",
                StpUtil.getLoginIdAsString(), request.getTransactionNo(), request.getRefundAmount(), request.getReason());
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), adminPayService.createRefund(request));
    }
}
