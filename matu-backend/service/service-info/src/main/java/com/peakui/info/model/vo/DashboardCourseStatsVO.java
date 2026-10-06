package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCourseStatsVO {
    @Builder.Default
    private Long courseCount = 0L;
    @Builder.Default
    private Long publishedCourseCount = 0L;
    @Builder.Default
    private Long studentCount = 0L;
    @Builder.Default
    private Long videoCount = 0L;
    @Builder.Default
    private Long articleCount = 0L;
    @Builder.Default
    private Long certificateCount = 0L;
}
