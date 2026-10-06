package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {
    private DashboardMetricVO metrics;
    private List<DashboardCheckinRankItemVO> checkinRanking;
    private DashboardCourseStatsVO courseStats;
    private DashboardOjStatsVO ojStats;
    private DashboardInterviewStatsVO interviewStats;
    private List<DashboardCertificationRankItemVO> certificationRankings;
    private String updatedAt;
}
