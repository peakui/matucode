package com.peakui.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardQaStatsVO {
    @Builder.Default
    private Long qaQuestionCount = 0L;
    @Builder.Default
    private Long qaResolvedCount = 0L;
}
