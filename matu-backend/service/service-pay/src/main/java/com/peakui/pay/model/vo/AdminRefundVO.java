package com.peakui.pay.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminRefundVO {
    private Long id;
    private String refundNo;
    private String transactionNo;
    private String orderNo;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private String channelRefundNo;
    private LocalDateTime refundTime;
    private LocalDateTime createdAt;
}
