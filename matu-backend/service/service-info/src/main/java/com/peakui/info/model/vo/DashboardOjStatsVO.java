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
public class DashboardOjStatsVO {
    @Builder.Default
    private Long problemCount = 0L;
    @Builder.Default
    private Long submissionCount = 0L;
    @Builder.Default
    private Long acceptedSubmissionCount = 0L;
    @Builder.Default
    private BigDecimal passRate = BigDecimal.ZERO;
    @Builder.Default
    private Long participantCount = 0L;
}
