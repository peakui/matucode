package com.peakui.info.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePlatformMetricRequest {

    @NotNull(message = "指标值不能为空")
    @Min(value = 0, message = "指标值不能小于0")
    private Long metricValue;

    private String extra;
}
