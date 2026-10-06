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
public class DashboardCheckinRankItemVO {
    private String userId;
    private String username;
    private String nickname;
    private String avatar;
    @Builder.Default
    private Long checkinCount = 0L;
    @Builder.Default
    private Integer continuousDays = 0;
    @Builder.Default
    private BigDecimal totalLearnHours = BigDecimal.ZERO;
}
