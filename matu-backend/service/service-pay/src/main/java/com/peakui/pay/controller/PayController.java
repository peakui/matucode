package com.peakui.pay.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.pay.model.dto.CreateAlipayPageRequest;
import com.peakui.pay.model.dto.CreateAlipayQrRequest;
import com.peakui.pay.model.dto.CreateOrderRequest;
import com.peakui.pay.model.vo.AlipayPagePayVO;
import com.peakui.pay.model.vo.AlipayQrPayVO;
import com.peakui.pay.model.vo.OrderVO;
import com.peakui.pay.model.vo.TransactionVO;
import com.peakui.pay.service.AlipayPayService;
import com.peakui.pay.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pay")
public class PayController {

    private final OrderService orderService;
    private final AlipayPayService alipayPayService;

    @Operation(summary = "创建支付订单")
    @PostMapping("/orders")
    public ApiResponse<OrderVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @Operation(summary = "查询支付订单")
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderVO> getOrder(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.getCurrentUserOrder(orderNo));
    }

    @Operation(summary = "创建支付宝扫码支付")
    @PostMapping("/alipay/qr")
    public ApiResponse<AlipayQrPayVO> createAlipayQr(@Valid @RequestBody CreateAlipayQrRequest request) {
        return ApiResponse.success(alipayPayService.createQrPay(request));
    }

    @Operation(summary = "创建支付宝电脑网页支付")
    @PostMapping("/alipay/page")
    public ApiResponse<AlipayPagePayVO> createAlipayPage(@Valid @RequestBody CreateAlipayPageRequest request) {
        return ApiResponse.success(alipayPayService.createPagePay(request));
    }

    @Operation(summary = "查询支付流水")
    @GetMapping("/transactions/{transactionNo}")
    public ApiResponse<TransactionVO> getTransaction(@PathVariable String transactionNo) {
        return ApiResponse.success(alipayPayService.getCurrentUserTransaction(transactionNo));
    }
}
