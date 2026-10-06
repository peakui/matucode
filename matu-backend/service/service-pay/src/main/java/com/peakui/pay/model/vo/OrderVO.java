package com.peakui.pay.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderVO {
    private String orderNo;
    private String productName;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payDeadline;
    private LocalDateTime createdAt;
}
