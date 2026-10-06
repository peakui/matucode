package com.peakui.pay.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActivateVipRequest {
    @NotBlank
    private String activationKey;
    @Min(1)
    private Integer days;
    @Min(1)
    private Integer level;
}
