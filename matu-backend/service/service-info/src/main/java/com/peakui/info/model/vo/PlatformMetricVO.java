package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformMetricVO {

    private Long id;
    private LocalDate metricDate;
    private String metricKey;
    private Long metricValue;
    private String extra;
    private LocalDateTime createdAt;
}
