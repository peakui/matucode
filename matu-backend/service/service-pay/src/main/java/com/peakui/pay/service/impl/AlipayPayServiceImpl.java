package com.peakui.pay.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.pay.config.AlipayProperties;
import com.peakui.pay.constant.OrderStatus;
import com.peakui.pay.constant.PayChannel;
import com.peakui.pay.constant.TransactionStatus;
import com.peakui.pay.exception.PayException;
import com.peakui.pay.mapper.BizOrderMapper;
import com.peakui.pay.mapper.PayTransactionMapper;
import com.peakui.pay.model.dto.CreateAlipayPageRequest;
import com.peakui.pay.model.dto.CreateAlipayQrRequest;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.entity.PayTransaction;
import com.peakui.pay.model.vo.AlipayPagePayVO;
import com.peakui.pay.model.vo.AlipayQrPayVO;
import com.peakui.pay.model.vo.TransactionVO;
import com.peakui.pay.service.AlipayPayService;
import com.peakui.pay.service.OrderService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPayServiceImpl implements AlipayPayService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final OrderService orderService;
    private final PayTransactionMapper payTransactionMapper;
    private final BizOrderMapper bizOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayQrPayVO createQrPay(CreateAlipayQrRequest request) {
        StpUtil.checkLogin();
        BizOrder order = orderService.getOwnedOrder(request.getOrderNo(), StpUtil.getLoginIdAsLong());
        if (OrderStatus.PENDING != order.getStatus()) {
            throw new PayException("订单状态不允许支付");
        }
        if (order.getPayDeadline() != null && order.getPayDeadline().isBefore(LocalDateTime.now())) {
            order.setStatus(OrderStatus.CLOSED);
            order.setUpdatedAt(LocalDateTime.now());
            bizOrderMapper.updateById(order);
            throw new PayException("订单已超时关闭");
        }

        PayTransaction transaction = getOrCreateTransaction(order);
        AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
        alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(transaction.getTransactionNo());
        model.setTotalAmount(transaction.getAmount().toPlainString());
        model.setSubject(order.getProductName());
        alipayRequest.setBizModel(model);

        try {
            AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);
            if (!response.isSuccess()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setUpdatedAt(LocalDateTime.now());
                payTransactionMapper.updateById(transaction);
                log.warn("支付宝预下单失败, orderNo={}, subMsg={}", order.getOrderNo(), response.getSubMsg());
                throw new PayException(response.getSubMsg() == null ? "支付宝预下单失败" : response.getSubMsg());
            }
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setUpdatedAt(LocalDateTime.now());
            payTransactionMapper.updateById(transaction);

            AlipayQrPayVO vo = new AlipayQrPayVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setTransactionNo(transaction.getTransactionNo());
            vo.setAmount(transaction.getAmount());
            vo.setQrCode(response.getQrCode());
            log.info("支付宝扫码支付预下单成功, orderNo={}, transactionNo={}, amount={}",
                    order.getOrderNo(), transaction.getTransactionNo(), transaction.getAmount());
            return vo;
        } catch (AlipayApiException e) {
            log.error("调用支付宝预下单异常, orderNo={}", order.getOrderNo(), e);
            throw new PayException("调用支付宝失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlipayPagePayVO createPagePay(CreateAlipayPageRequest request) {
        StpUtil.checkLogin();
        BizOrder order = orderService.getOwnedOrder(request.getOrderNo(), StpUtil.getLoginIdAsLong());
        if (OrderStatus.PENDING != order.getStatus()) {
            throw new PayException("订单状态不允许支付");
        }
        if (order.getPayDeadline() != null && order.getPayDeadline().isBefore(LocalDateTime.now())) {
            order.setStatus(OrderStatus.CLOSED);
            order.setUpdatedAt(LocalDateTime.now());
            bizOrderMapper.updateById(order);
            throw new PayException("订单已超时关闭");
        }

        PayTransaction transaction = getOrCreateTransaction(order);
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setNotifyUrl(alipayProperties.getNotifyUrl());
        alipayRequest.setReturnUrl(alipayProperties.getReturnUrl());
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(transaction.getTransactionNo());
        model.setTotalAmount(transaction.getAmount().toPlainString());
        model.setSubject(order.getProductName());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        alipayRequest.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
            if (!response.isSuccess()) {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setUpdatedAt(LocalDateTime.now());
                payTransactionMapper.updateById(transaction);
                log.warn("支付宝网页支付创建失败, orderNo={}, subMsg={}", order.getOrderNo(), response.getSubMsg());
                throw new PayException(response.getSubMsg() == null ? "支付宝网页支付创建失败" : response.getSubMsg());
            }
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setUpdatedAt(LocalDateTime.now());
            payTransactionMapper.updateById(transaction);

            AlipayPagePayVO vo = new AlipayPagePayVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setTransactionNo(transaction.getTransactionNo());
            vo.setAmount(transaction.getAmount());
            vo.setPayForm(response.getBody());
            log.info("支付宝网页支付创建成功, orderNo={}, transactionNo={}, amount={}",
                    order.getOrderNo(), transaction.getTransactionNo(), transaction.getAmount());
            return vo;
        } catch (AlipayApiException e) {
            log.error("调用支付宝网页支付异常, orderNo={}", order.getOrderNo(), e);
            throw new PayException("调用支付宝网页支付失败");
        }
    }

    @Override
    public TransactionVO getCurrentUserTransaction(String transactionNo) {
        StpUtil.checkLogin();
        PayTransaction transaction = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getTransactionNo, transactionNo)
                .last("limit 1"));
        if (transaction == null) {
            throw new PayException("支付流水不存在");
        }
        orderService.getOwnedOrder(transaction.getOrderNo(), StpUtil.getLoginIdAsLong());
        return toVO(transaction);
    }

    private PayTransaction getOrCreateTransaction(BizOrder order) {
        PayTransaction transaction = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getOrderNo, order.getOrderNo())
                .in(PayTransaction::getStatus, TransactionStatus.PENDING, TransactionStatus.PROCESSING)
                .last("limit 1"));
        if (transaction != null) {
            return transaction;
        }
        LocalDateTime now = LocalDateTime.now();
        transaction = new PayTransaction();
        transaction.setTransactionNo(generateNo("T"));
        transaction.setOrderNo(order.getOrderNo());
        transaction.setChannel(PayChannel.ALIPAY);
        transaction.setAmount(order.getAmount());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        payTransactionMapper.insert(transaction);
        return transaction;
    }

    private TransactionVO toVO(PayTransaction transaction) {
        TransactionVO vo = new TransactionVO();
        vo.setTransactionNo(transaction.getTransactionNo());
        vo.setOrderNo(transaction.getOrderNo());
        vo.setChannel(transaction.getChannel());
        vo.setChannelTradeNo(transaction.getChannelTradeNo());
        vo.setAmount(transaction.getAmount());
        vo.setStatus(transaction.getStatus());
        vo.setPayTime(transaction.getPayTime());
        vo.setCreatedAt(transaction.getCreatedAt());
        return vo;
    }

    private String generateNo(String prefix) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return prefix + time + suffix;
    }
}
