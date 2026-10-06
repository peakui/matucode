package com.peakui.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInterviewStatsDTO {
    @Builder.Default
    private Long questionCount = 0L;
    @Builder.Default
    private Long categoryCount = 0L;
    @Builder.Default
    private Long companyCount = 0L;
    @Builder.Default
    private Long lockedQuestionCount = 0L;
}
