package com.peakui.course.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProgressRequest {
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private BigDecimal progressPercent;
    private Integer watchedDuration;
}
