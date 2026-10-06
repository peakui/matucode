package com.peakui.pay.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAlipayPageRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}
