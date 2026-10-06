package com.peakui.pay.service;

import com.peakui.common.result.PageResponse;
import com.peakui.pay.model.dto.AdminRefundRequest;
import com.peakui.pay.model.vo.AdminOrderVO;
import com.peakui.pay.model.vo.AdminRefundVO;
import com.peakui.pay.model.vo.AdminTransactionVO;
import com.peakui.pay.model.vo.RefundVO;

import java.time.LocalDateTime;

public interface AdminPayService {

    PageResponse<AdminOrderVO> listOrders(String orderNo, Long userId, Integer status, String productName,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Long pageNum, Long pageSize);

    AdminOrderVO getOrder(String orderNo);

    PageResponse<AdminTransactionVO> listTransactions(String transactionNo, String orderNo, String channel, Integer status,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      Long pageNum, Long pageSize);

    PageResponse<AdminRefundVO> listRefunds(String refundNo, String transactionNo, String orderNo, Integer status,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            Long pageNum, Long pageSize);

    RefundVO createRefund(AdminRefundRequest request);
}
