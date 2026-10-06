package com.peakui.pay.service.impl;

import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.pay.config.AlipayProperties;
import com.peakui.pay.constant.OrderStatus;
import com.peakui.pay.constant.PayChannel;
import com.peakui.pay.constant.TransactionStatus;
import com.peakui.pay.feign.AuthFeignClient;
import com.peakui.pay.mapper.AlipayNotifyLogMapper;
import com.peakui.pay.mapper.BizOrderMapper;
import com.peakui.pay.mapper.PayTransactionMapper;
import com.peakui.pay.model.dto.ActivateVipRequest;
import com.peakui.pay.model.entity.AlipayNotifyLog;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.entity.PayTransaction;
import com.peakui.pay.service.AlipayNotifyService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayNotifyServiceImpl implements AlipayNotifyService {
    private final AlipayProperties alipayProperties;
    private final AlipayNotifyLogMapper alipayNotifyLogMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final BizOrderMapper bizOrderMapper;
    private final AuthFeignClient authFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleNotify(Map<String, String> params) {
        // Always authenticate the entire payload, even when notify_id has been seen before.
        try {
            if (!AlipaySignature.rsaCheckV1(params, alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(), alipayProperties.getSignType())
                    || !Objects.equals(alipayProperties.getAppId(), params.get("app_id"))) {
                log.warn("支付宝回调验签未通过");
                return false;
            }
        } catch (Exception e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return true;
        }
        if (!StringUtils.hasText(params.get("out_trade_no"))
                || !StringUtils.hasText(params.get("trade_no"))
                || !StringUtils.hasText(params.get("total_amount"))) {
            return false;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(params.get("total_amount"));
        } catch (NumberFormatException e) {
            log.warn("支付宝回调金额非法, outTradeNo={}", params.get("out_trade_no"));
            return false;
        }
        PayTransaction candidate = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getTransactionNo, params.get("out_trade_no")).last("limit 1"));
        if (candidate == null) {
            log.warn("支付宝回调找不到支付流水, outTradeNo={}", params.get("out_trade_no"));
            return false;
        }
        // The order lock serializes callbacks and payment creation for the same order.
        BizOrder order = bizOrderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getOrderNo, candidate.getOrderNo()).last("limit 1 for update"));
        PayTransaction transaction = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getTransactionNo, candidate.getTransactionNo()).last("limit 1 for update"));
        if (order == null || transaction == null || !PayChannel.ALIPAY.equals(transaction.getChannel())
                || transaction.getAmount() == null || order.getAmount() == null
                || amount.compareTo(transaction.getAmount()) != 0 || amount.compareTo(order.getAmount()) != 0
                || (StringUtils.hasText(transaction.getChannelTradeNo())
                    && !transaction.getChannelTradeNo().equals(params.get("trade_no")))) {
            log.warn("支付宝回调校验不通过, orderNo={}, outTradeNo={}",
                    candidate.getOrderNo(), params.get("out_trade_no"));
            return false;
        }
        // Never re-grant membership or overwrite a refunded order on a delayed callback.
        if (Objects.equals(order.getStatus(), OrderStatus.REFUNDED)
                || Objects.equals(order.getStatus(), OrderStatus.PARTIAL_REFUNDED)) {
            log.warn("支付宝回调命中已退款订单，忽略开通, orderNo={}", order.getOrderNo());
            return Objects.equals(transaction.getStatus(), TransactionStatus.SUCCESS);
        }
        LocalDateTime payTime = parseAlipayTime(params.get("gmt_payment"));
        ActivateVipRequest grant = new ActivateVipRequest();
        // An order, rather than a payment attempt or notify_id, is the fulfillment identity.
        grant.setActivationKey("pay:" + order.getOrderNo());
        grant.setDays(resolveVipDays(order.getProductName()));
        grant.setLevel(1);
        var response = authFeignClient.activateVip(order.getUserId(), grant);
        if (response == null || !Integer.valueOf(0).equals(response.getCode())) {
            log.error("支付宝回调开通会员失败, orderNo={}, userId={}, resp={}",
                    order.getOrderNo(), order.getUserId(), response);
            throw new IllegalStateException("会员开通失败，请重试支付通知");
        }
        // If this transaction fails after auth commits, a retry uses the same permanent key.
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setChannelTradeNo(params.get("trade_no"));
        transaction.setPayTime(payTime == null ? LocalDateTime.now() : payTime);
        transaction.setNotifyContent(toRawBody(params));
        transaction.setUpdatedAt(LocalDateTime.now());
        if (payTransactionMapper.updateById(transaction) != 1) {
            throw new IllegalStateException("支付流水更新失败");
        }
        if (!Objects.equals(order.getStatus(), OrderStatus.PAID)) {
            order.setStatus(OrderStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            if (bizOrderMapper.updateById(order) != 1) {
                throw new IllegalStateException("支付订单更新失败");
            }
        }
        saveProcessedLog(params);
        log.info("支付宝回调处理成功, orderNo={}, tradeNo={}, userId={}, vipDays={}",
                order.getOrderNo(), params.get("trade_no"), order.getUserId(), grant.getDays());
        return true;
    }

    private int resolveVipDays(String productName) {
        // Existing orders persist the server-generated product name, never a client-supplied duration.
        if (productName == null) throw new IllegalArgumentException("缺少会员商品");
        return switch (productName) {
            case "VIP月卡" -> 31;
            case "VIP季卡" -> 93;
            case "VIP年卡" -> 366;
            default -> throw new IllegalArgumentException("不支持的会员商品");
        };
    }

    private void saveProcessedLog(Map<String, String> params) {
        String notifyId = params.get("notify_id");
        AlipayNotifyLog record = StringUtils.hasText(notifyId)
                ? alipayNotifyLogMapper.selectOne(new LambdaQueryWrapper<AlipayNotifyLog>()
                    .eq(AlipayNotifyLog::getNotifyId, notifyId).last("limit 1")) : null;
        boolean insert = record == null;
        if (insert) {
            record = new AlipayNotifyLog();
            record.setNotifyId(StringUtils.hasText(notifyId) ? notifyId : "N" + System.nanoTime());
            record.setCreatedAt(LocalDateTime.now());
        }
        record.setTradeNo(params.get("trade_no"));
        record.setOutTradeNo(params.get("out_trade_no"));
        record.setRawBody(toRawBody(params));
        record.setSignVerified(1);
        record.setProcessed(1);
        if (insert) alipayNotifyLogMapper.insert(record);
        else alipayNotifyLogMapper.updateById(record);
    }

    private String toRawBody(Map<String, String> params) {
        return params.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private LocalDateTime parseAlipayTime(String value) {
        return !StringUtils.hasText(value) ? null
                : LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
