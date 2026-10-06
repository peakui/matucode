package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricVO {
    @Builder.Default
    private Long articleCount = 0L;
    @Builder.Default
    private Long checkinCount = 0L;
    @Builder.Default
    private Long qaQuestionCount = 0L;
    @Builder.Default
    private Long qaResolvedCount = 0L;
    @Builder.Default
    private BigDecimal qaResolveRate = BigDecimal.ZERO;
    @Builder.Default
    private Long interviewQuestionCount = 0L;
    @Builder.Default
    private Long certificationCount = 0L;
}
