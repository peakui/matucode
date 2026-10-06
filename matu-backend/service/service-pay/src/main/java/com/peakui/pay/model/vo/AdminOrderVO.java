package com.peakui.pay.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String productName;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
