package com.peakui.pay.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminTransactionVO {
    private Long id;
    private String transactionNo;
    private String orderNo;
    private String channel;
    private String channelTradeNo;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
}
