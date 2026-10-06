package com.peakui.info.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePlatformMetricRequest {

    @NotNull(message = "统计日期不能为空")
    private LocalDate metricDate;

    @NotBlank(message = "指标名不能为空")
    @Size(max = 64, message = "指标名长度不能超过64个字符")
    private String metricKey;

    @NotNull(message = "指标值不能为空")
    @Min(value = 0, message = "指标值不能小于0")
    private Long metricValue;

    private String extra;
}
