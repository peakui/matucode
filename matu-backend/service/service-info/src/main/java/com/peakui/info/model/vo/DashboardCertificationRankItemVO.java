package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCertificationRankItemVO {
    private String name;
    private String type;
    @Builder.Default
    private Long count = 0L;
}
