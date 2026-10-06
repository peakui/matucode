package com.peakui.pay.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "商品编码不能为空")
    private String productCode;
}
