package com.peakui.pay.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminRefundRequest {
    @NotBlank(message = "支付流水号不能为空")
    private String transactionNo;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款原因不能为空")
    private String reason;
}
