package com.peakui.pay.service;

import com.peakui.pay.model.dto.CreateOrderRequest;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.vo.OrderVO;

public interface OrderService {
    OrderVO createOrder(CreateOrderRequest request);

    OrderVO getCurrentUserOrder(String orderNo);

    BizOrder getOwnedOrder(String orderNo, Long userId);
}
