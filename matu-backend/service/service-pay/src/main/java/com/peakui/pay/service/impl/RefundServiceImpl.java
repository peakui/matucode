package com.peakui.pay.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.pay.constant.OrderStatus;
import com.peakui.pay.constant.RefundStatus;
import com.peakui.pay.constant.TransactionStatus;
import com.peakui.pay.exception.PayException;
import com.peakui.pay.mapper.BizOrderMapper;
import com.peakui.pay.mapper.PayRefundMapper;
import com.peakui.pay.mapper.PayTransactionMapper;
import com.peakui.pay.model.dto.RefundRequest;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.entity.PayRefund;
import com.peakui.pay.model.entity.PayTransaction;
import com.peakui.pay.model.vo.RefundVO;
import com.peakui.pay.service.OrderService;
import com.peakui.pay.service.RefundService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final AlipayClient alipayClient;
    private final PayTransactionMapper payTransactionMapper;
    private final PayRefundMapper payRefundMapper;
    private final BizOrderMapper bizOrderMapper;
    private final OrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO refund(RefundRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用户申请退款, userId={}, transactionNo={}, amount={}",
                userId, request.getTransactionNo(), request.getRefundAmount());
        return doRefund(request, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO refundAsAdmin(RefundRequest request) {
        log.info("管理员发起退款, transactionNo={}, amount={}",
                request.getTransactionNo(), request.getRefundAmount());
        return doRefund(request, null);
    }

    /**
     * @param requiredOwnerId the user the order must belong to; {@code null} for an admin
     *                        refund, which deliberately skips the ownership check.
     */
    private RefundVO doRefund(RefundRequest request, Long requiredOwnerId) {
        PayTransaction transaction = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getTransactionNo, request.getTransactionNo())
                .last("limit 1"));
        if (transaction == null) {
            throw new PayException("支付流水不存在");
        }
        if (TransactionStatus.SUCCESS != transaction.getStatus()) {
            throw new PayException("支付流水状态不允许退款");
        }
        BizOrder order = requiredOwnerId == null
                ? bizOrderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                        .eq(BizOrder::getOrderNo, transaction.getOrderNo())
                        .last("limit 1"))
                : orderService.getOwnedOrder(transaction.getOrderNo(), requiredOwnerId);
        if (order == null) {
            throw new PayException("订单不存在");
        }
        BigDecimal refundedAmount = successfulRefunds(transaction.getTransactionNo()).stream()
                .map(PayRefund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refundedAmount.add(request.getRefundAmount()).compareTo(transaction.getAmount()) > 0) {
            throw new PayException("退款金额超过可退金额");
        }

        PayRefund refund = new PayRefund();
        refund.setRefundNo(generateNo("R"));
        refund.setTransactionNo(transaction.getTransactionNo());
        refund.setOrderNo(transaction.getOrderNo());
        refund.setRefundAmount(request.getRefundAmount());
        refund.setReason(request.getReason());
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());
        payRefundMapper.insert(refund);

        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(transaction.getTransactionNo());
        model.setRefundAmount(request.getRefundAmount().toPlainString());
        model.setRefundReason(request.getReason());
        model.setOutRequestNo(refund.getRefundNo());
        alipayRequest.setBizModel(model);

        try {
            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
            if (!response.isSuccess()) {
                refund.setStatus(RefundStatus.FAILED);
                refund.setUpdatedAt(LocalDateTime.now());
                payRefundMapper.updateById(refund);
                log.warn("支付宝退款失败, refundNo={}, subMsg={}", refund.getRefundNo(), response.getSubMsg());
                throw new PayException(response.getSubMsg() == null ? "支付宝退款失败" : response.getSubMsg());
            }
            refund.setStatus(RefundStatus.SUCCESS);
            refund.setChannelRefundNo(response.getTradeNo());
            refund.setRefundTime(LocalDateTime.now());
            refund.setUpdatedAt(LocalDateTime.now());
            payRefundMapper.updateById(refund);

            BigDecimal totalRefunded = refundedAmount.add(request.getRefundAmount());
            order.setStatus(totalRefunded.compareTo(order.getAmount()) >= 0
                    ? OrderStatus.REFUNDED
                    : OrderStatus.PARTIAL_REFUNDED);
            order.setUpdatedAt(LocalDateTime.now());
            bizOrderMapper.updateById(order);
            log.info("退款成功, refundNo={}, orderNo={}, amount={}, 累计已退={}, orderStatus={}",
                    refund.getRefundNo(), order.getOrderNo(), request.getRefundAmount(),
                    totalRefunded, order.getStatus());
            return toVO(refund);
        } catch (AlipayApiException e) {
            log.error("调用支付宝退款异常, refundNo={}, transactionNo={}",
                    refund.getRefundNo(), request.getTransactionNo(), e);
            throw new PayException("调用支付宝退款失败");
        }
    }

    private List<PayRefund> successfulRefunds(String transactionNo) {
        return payRefundMapper.selectList(new LambdaQueryWrapper<PayRefund>()
                .eq(PayRefund::getTransactionNo, transactionNo)
                .eq(PayRefund::getStatus, RefundStatus.SUCCESS));
    }

    private RefundVO toVO(PayRefund refund) {
        RefundVO vo = new RefundVO();
        vo.setRefundNo(refund.getRefundNo());
        vo.setTransactionNo(refund.getTransactionNo());
        vo.setOrderNo(refund.getOrderNo());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setReason(refund.getReason());
        vo.setStatus(refund.getStatus());
        vo.setChannelRefundNo(refund.getChannelRefundNo());
        vo.setRefundTime(refund.getRefundTime());
        vo.setCreatedAt(refund.getCreatedAt());
        return vo;
    }

    private String generateNo(String prefix) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return prefix + time + suffix;
    }
}
