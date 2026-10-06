package com.peakui.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAuthStatsVO {
    @Builder.Default
    private Long certificationCount = 0L;
    @Builder.Default
    private List<DashboardCertificationRankItemDTO> certificationRankings = List.of();
}
