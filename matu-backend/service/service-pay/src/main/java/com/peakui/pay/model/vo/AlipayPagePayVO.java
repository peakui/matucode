package com.peakui.pay.model.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AlipayPagePayVO {
    private String orderNo;
    private String transactionNo;
    private BigDecimal amount;
    private String payForm;
}
