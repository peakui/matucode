package com.peakui.pay.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.pay.constant.OrderStatus;
import com.peakui.pay.constant.PayProduct;
import com.peakui.pay.exception.PayException;
import com.peakui.pay.mapper.BizOrderMapper;
import com.peakui.pay.model.dto.CreateOrderRequest;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.vo.OrderVO;
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
public class OrderServiceImpl implements OrderService {

    private final BizOrderMapper bizOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        PayProduct product = PayProduct.of(request.getProductCode());
        LocalDateTime now = LocalDateTime.now();

        BizOrder order = new BizOrder();
        order.setOrderNo(generateNo("O"));
        order.setUserId(userId);
        order.setProductName(product.productName());
        order.setAmount(product.amount());
        order.setStatus(OrderStatus.PENDING);
        order.setPayDeadline(now.plusMinutes(30));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        bizOrderMapper.insert(order);
        log.info("创建支付订单成功, orderNo={}, userId={}, product={}, amount={}",
                order.getOrderNo(), userId, request.getProductCode(), order.getAmount());
        return toVO(order);
    }

    @Override
    public OrderVO getCurrentUserOrder(String orderNo) {
        StpUtil.checkLogin();
        return toVO(getOwnedOrder(orderNo, StpUtil.getLoginIdAsLong()));
    }

    @Override
    public BizOrder getOwnedOrder(String orderNo, Long userId) {
        BizOrder order = bizOrderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getOrderNo, orderNo)
                .eq(BizOrder::getUserId, userId)
                .last("limit 1"));
        if (order == null) {
            throw new PayException("订单不存在");
        }
        return order;
    }

    private OrderVO toVO(BizOrder order) {
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setProductName(order.getProductName());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setPayDeadline(order.getPayDeadline());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private String generateNo(String prefix) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return prefix + time + suffix;
    }
}
