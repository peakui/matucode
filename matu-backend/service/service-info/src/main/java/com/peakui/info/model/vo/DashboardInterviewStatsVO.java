package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInterviewStatsVO {
    @Builder.Default
    private Long questionCount = 0L;
    @Builder.Default
    private Long categoryCount = 0L;
    @Builder.Default
    private Long companyCount = 0L;
    @Builder.Default
    private Long lockedQuestionCount = 0L;
}
