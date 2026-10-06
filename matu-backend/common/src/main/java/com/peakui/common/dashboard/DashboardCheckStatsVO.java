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
public class DashboardCheckStatsVO {
    @Builder.Default
    private Long checkinCount = 0L;
    @Builder.Default
    private List<DashboardCheckinRankItemDTO> checkinRanking = List.of();
}
