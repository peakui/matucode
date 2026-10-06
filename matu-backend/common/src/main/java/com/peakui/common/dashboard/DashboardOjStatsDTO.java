package com.peakui.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOjStatsDTO {
    @Builder.Default
    private Long problemCount = 0L;
    @Builder.Default
    private Long submissionCount = 0L;
    @Builder.Default
    private Long acceptedSubmissionCount = 0L;
    @Builder.Default
    private Long participantCount = 0L;
}
