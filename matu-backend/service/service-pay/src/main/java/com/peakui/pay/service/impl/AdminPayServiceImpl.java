package com.peakui.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.pay.exception.PayException;
import com.peakui.pay.mapper.BizOrderMapper;
import com.peakui.pay.mapper.PayRefundMapper;
import com.peakui.pay.mapper.PayTransactionMapper;
import com.peakui.pay.model.dto.AdminRefundRequest;
import com.peakui.pay.model.dto.RefundRequest;
import com.peakui.pay.model.entity.BizOrder;
import com.peakui.pay.model.entity.PayRefund;
import com.peakui.pay.model.entity.PayTransaction;
import com.peakui.pay.model.vo.AdminOrderVO;
import com.peakui.pay.model.vo.AdminRefundVO;
import com.peakui.pay.model.vo.AdminTransactionVO;
import com.peakui.pay.model.vo.RefundVO;
import com.peakui.pay.service.AdminPayService;
import com.peakui.pay.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPayServiceImpl implements AdminPayService {

    private static final long MAX_PAGE_SIZE = 100;

    private final BizOrderMapper bizOrderMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final PayRefundMapper payRefundMapper;
    private final RefundService refundService;

    @Override
    public PageResponse<AdminOrderVO> listOrders(String orderNo, Long userId, Integer status, String productName,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 Long pageNum, Long pageSize) {
        Page<BizOrder> page = bizOrderMapper.selectPage(this.<BizOrder>buildPage(pageNum, pageSize),
                new LambdaQueryWrapper<BizOrder>()
                        .like(StringUtils.hasText(orderNo), BizOrder::getOrderNo, orderNo)
                        .eq(userId != null, BizOrder::getUserId, userId)
                        .eq(status != null, BizOrder::getStatus, status)
                        .like(StringUtils.hasText(productName), BizOrder::getProductName, productName)
                        .ge(startTime != null, BizOrder::getCreatedAt, startTime)
                        .le(endTime != null, BizOrder::getCreatedAt, endTime)
                        .orderByDesc(BizOrder::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toOrderVO).toList());
    }

    @Override
    public AdminOrderVO getOrder(String orderNo) {
        BizOrder order = bizOrderMapper.selectOne(new LambdaQueryWrapper<BizOrder>()
                .eq(BizOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            throw new PayException("订单不存在");
        }
        return toOrderVO(order);
    }

    @Override
    public PageResponse<AdminTransactionVO> listTransactions(String transactionNo, String orderNo, String channel, Integer status,
                                                             LocalDateTime startTime, LocalDateTime endTime,
                                                             Long pageNum, Long pageSize) {
        Page<PayTransaction> page = payTransactionMapper.selectPage(this.<PayTransaction>buildPage(pageNum, pageSize),
                new LambdaQueryWrapper<PayTransaction>()
                        .like(StringUtils.hasText(transactionNo), PayTransaction::getTransactionNo, transactionNo)
                        .like(StringUtils.hasText(orderNo), PayTransaction::getOrderNo, orderNo)
                        .eq(StringUtils.hasText(channel), PayTransaction::getChannel, channel)
                        .eq(status != null, PayTransaction::getStatus, status)
                        .ge(startTime != null, PayTransaction::getCreatedAt, startTime)
                        .le(endTime != null, PayTransaction::getCreatedAt, endTime)
                        .orderByDesc(PayTransaction::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toTransactionVO).toList());
    }

    @Override
    public PageResponse<AdminRefundVO> listRefunds(String refundNo, String transactionNo, String orderNo, Integer status,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Long pageNum, Long pageSize) {
        Page<PayRefund> page = payRefundMapper.selectPage(this.<PayRefund>buildPage(pageNum, pageSize),
                new LambdaQueryWrapper<PayRefund>()
                        .like(StringUtils.hasText(refundNo), PayRefund::getRefundNo, refundNo)
                        .like(StringUtils.hasText(transactionNo), PayRefund::getTransactionNo, transactionNo)
                        .like(StringUtils.hasText(orderNo), PayRefund::getOrderNo, orderNo)
                        .eq(status != null, PayRefund::getStatus, status)
                        .ge(startTime != null, PayRefund::getCreatedAt, startTime)
                        .le(endTime != null, PayRefund::getCreatedAt, endTime)
                        .orderByDesc(PayRefund::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toRefundVO).toList());
    }

    @Override
    public RefundVO createRefund(AdminRefundRequest request) {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setTransactionNo(request.getTransactionNo());
        refundRequest.setRefundAmount(request.getRefundAmount());
        refundRequest.setReason(request.getReason());
        return refundService.refundAsAdmin(refundRequest);
    }

    private <T> Page<T> buildPage(Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(current, size);
    }

    private AdminOrderVO toOrderVO(BizOrder order) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setProductName(order.getProductName());
        vo.setAmount(order.getAmount());
        vo.setStatus(order.getStatus());
        vo.setPayDeadline(order.getPayDeadline());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());
        return vo;
    }

    private AdminTransactionVO toTransactionVO(PayTransaction transaction) {
        AdminTransactionVO vo = new AdminTransactionVO();
        vo.setId(transaction.getId());
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

    private AdminRefundVO toRefundVO(PayRefund refund) {
        AdminRefundVO vo = new AdminRefundVO();
        vo.setId(refund.getId());
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
}
