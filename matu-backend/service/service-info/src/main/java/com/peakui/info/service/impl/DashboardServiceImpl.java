package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.dashboard.DashboardAuthStatsVO;
import com.peakui.common.dashboard.DashboardCertificationRankItemDTO;
import com.peakui.common.dashboard.DashboardCheckStatsVO;
import com.peakui.common.dashboard.DashboardCheckinRankItemDTO;
import com.peakui.common.dashboard.DashboardCourseStatsDTO;
import com.peakui.common.dashboard.DashboardInterviewStatsDTO;
import com.peakui.common.dashboard.DashboardOjStatsDTO;
import com.peakui.common.dashboard.DashboardPostStatsVO;
import com.peakui.common.dashboard.DashboardQaStatsVO;
import com.peakui.common.result.ApiResponse;
import com.peakui.info.feign.AuthDashboardFeignClient;
import com.peakui.info.feign.CheckDashboardFeignClient;
import com.peakui.info.feign.CourseDashboardFeignClient;
import com.peakui.info.feign.InterviewDashboardFeignClient;
import com.peakui.info.feign.OjDashboardFeignClient;
import com.peakui.info.feign.PostDashboardFeignClient;
import com.peakui.info.feign.QaDashboardFeignClient;
import com.peakui.info.model.vo.DashboardCertificationRankItemVO;
import com.peakui.info.model.vo.DashboardCheckinRankItemVO;
import com.peakui.info.model.vo.DashboardCourseStatsVO;
import com.peakui.info.model.vo.DashboardInterviewStatsVO;
import com.peakui.info.model.vo.DashboardMetricVO;
import com.peakui.info.model.vo.DashboardOjStatsVO;
import com.peakui.info.model.vo.DashboardOverviewVO;
import com.peakui.info.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PostDashboardFeignClient postDashboardFeignClient;
    private final CheckDashboardFeignClient checkDashboardFeignClient;
    private final QaDashboardFeignClient qaDashboardFeignClient;
    private final CourseDashboardFeignClient courseDashboardFeignClient;
    private final OjDashboardFeignClient ojDashboardFeignClient;
    private final InterviewDashboardFeignClient interviewDashboardFeignClient;
    private final AuthDashboardFeignClient authDashboardFeignClient;

    @Override
    public DashboardOverviewVO getOverview() {
        StpUtil.checkLogin();
        StpUtil.checkRole("ADMIN");

        DashboardPostStatsVO postStats = fetch(postDashboardFeignClient::getDashboardStats, DashboardPostStatsVO.builder().build());
        DashboardCheckStatsVO checkStats = fetch(checkDashboardFeignClient::getDashboardStats, DashboardCheckStatsVO.builder().build());
        DashboardQaStatsVO qaStats = fetch(qaDashboardFeignClient::getDashboardStats, DashboardQaStatsVO.builder().build());
        DashboardCourseStatsDTO courseStats = fetch(courseDashboardFeignClient::getDashboardStats, DashboardCourseStatsDTO.builder().build());
        DashboardOjStatsDTO ojStats = fetch(ojDashboardFeignClient::getDashboardStats, DashboardOjStatsDTO.builder().build());
        DashboardInterviewStatsDTO interviewStats = fetch(interviewDashboardFeignClient::getDashboardStats, DashboardInterviewStatsDTO.builder().build());
        DashboardAuthStatsVO authStats = fetch(authDashboardFeignClient::getDashboardStats, DashboardAuthStatsVO.builder().build());

        return DashboardOverviewVO.builder()
                .metrics(buildMetrics(postStats, checkStats, qaStats, interviewStats, authStats))
                .checkinRanking(toCheckinRanking(checkStats.getCheckinRanking()))
                .courseStats(toCourseStats(courseStats))
                .ojStats(toOjStats(ojStats))
                .interviewStats(toInterviewStats(interviewStats))
                .certificationRankings(toCertificationRankings(authStats.getCertificationRankings()))
                .updatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .build();
    }

    private DashboardMetricVO buildMetrics(DashboardPostStatsVO postStats,
                                           DashboardCheckStatsVO checkStats,
                                           DashboardQaStatsVO qaStats,
                                           DashboardInterviewStatsDTO interviewStats,
                                           DashboardAuthStatsVO authStats) {
        return DashboardMetricVO.builder()
                .articleCount(safeLong(postStats.getArticleCount()))
                .checkinCount(safeLong(checkStats.getCheckinCount()))
                .qaQuestionCount(safeLong(qaStats.getQaQuestionCount()))
                .qaResolvedCount(safeLong(qaStats.getQaResolvedCount()))
                .qaResolveRate(calculateRate(qaStats.getQaResolvedCount(), qaStats.getQaQuestionCount()))
                .interviewQuestionCount(safeLong(interviewStats.getQuestionCount()))
                .certificationCount(safeLong(authStats.getCertificationCount()))
                .build();
    }

    private DashboardCourseStatsVO toCourseStats(DashboardCourseStatsDTO stats) {
        return DashboardCourseStatsVO.builder()
                .courseCount(safeLong(stats.getCourseCount()))
                .publishedCourseCount(safeLong(stats.getPublishedCourseCount()))
                .studentCount(safeLong(stats.getStudentCount()))
                .videoCount(safeLong(stats.getVideoCount()))
                .articleCount(safeLong(stats.getArticleCount()))
                .certificateCount(safeLong(stats.getCertificateCount()))
                .build();
    }

    private DashboardOjStatsVO toOjStats(DashboardOjStatsDTO stats) {
        return DashboardOjStatsVO.builder()
                .problemCount(safeLong(stats.getProblemCount()))
                .submissionCount(safeLong(stats.getSubmissionCount()))
                .acceptedSubmissionCount(safeLong(stats.getAcceptedSubmissionCount()))
                .passRate(calculateRate(stats.getAcceptedSubmissionCount(), stats.getSubmissionCount()))
                .participantCount(safeLong(stats.getParticipantCount()))
                .build();
    }

    private DashboardInterviewStatsVO toInterviewStats(DashboardInterviewStatsDTO stats) {
        return DashboardInterviewStatsVO.builder()
                .questionCount(safeLong(stats.getQuestionCount()))
                .categoryCount(safeLong(stats.getCategoryCount()))
                .companyCount(safeLong(stats.getCompanyCount()))
                .lockedQuestionCount(safeLong(stats.getLockedQuestionCount()))
                .build();
    }

    private List<DashboardCheckinRankItemVO> toCheckinRanking(List<DashboardCheckinRankItemDTO> ranking) {
        if (ranking == null) {
            return List.of();
        }
        return ranking.stream()
                .map(item -> DashboardCheckinRankItemVO.builder()
                        .userId(item.getUserId())
                        .username(item.getUsername())
                        .nickname(item.getNickname())
                        .avatar(item.getAvatar())
                        .checkinCount(safeLong(item.getCheckinCount()))
                        .continuousDays(item.getContinuousDays() == null ? 0 : item.getContinuousDays())
                        .totalLearnHours(item.getTotalLearnHours() == null ? BigDecimal.ZERO : item.getTotalLearnHours())
                        .build())
                .toList();
    }

    private List<DashboardCertificationRankItemVO> toCertificationRankings(List<DashboardCertificationRankItemDTO> rankings) {
        if (rankings == null) {
            return List.of();
        }
        return rankings.stream()
                .map(item -> DashboardCertificationRankItemVO.builder()
                        .name(item.getName())
                        .type(item.getType())
                        .count(safeLong(item.getCount()))
                        .build())
                .toList();
    }

    private <T> T fetch(Supplier<ApiResponse<T>> supplier, T defaultValue) {
        try {
            ApiResponse<T> response = supplier.get();
            if (response == null || response.getData() == null) {
                return defaultValue;
            }
            return response.getData();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal calculateRate(Long numerator, Long denominator) {
        long divisor = safeLong(denominator);
        if (divisor == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(safeLong(numerator))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
