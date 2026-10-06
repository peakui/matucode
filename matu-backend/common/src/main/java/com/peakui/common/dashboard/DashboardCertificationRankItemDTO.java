package com.peakui.common.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCertificationRankItemDTO {
    private String name;
    private String type;
    @Builder.Default
    private Long count = 0L;
}
