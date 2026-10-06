package com.peakui.oj.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.common.dashboard.DashboardOjStatsDTO;
import com.peakui.common.result.ApiResponse;
import com.peakui.oj.mapper.OjProblemMapper;
import com.peakui.oj.mapper.OjSubmissionMapper;
import com.peakui.oj.model.entity.OjProblem;
import com.peakui.oj.model.entity.OjSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OJ数据总览")
@RestController
@RequiredArgsConstructor
@RequestMapping("/oj/internal/dashboard")
public class OjDashboardController {

    private final OjProblemMapper ojProblemMapper;
    private final OjSubmissionMapper ojSubmissionMapper;

    @Operation(summary = "OJ数据总览统计")
    @GetMapping("/stats")
    public ApiResponse<DashboardOjStatsDTO> stats() {
        return ApiResponse.success(DashboardOjStatsDTO.builder()
                .problemCount(ojProblemMapper.selectCount(new LambdaQueryWrapper<OjProblem>().eq(OjProblem::getStatus, 1)))
                .submissionCount(ojSubmissionMapper.selectCount(null))
                .acceptedSubmissionCount(ojSubmissionMapper.selectCount(new LambdaQueryWrapper<OjSubmission>().eq(OjSubmission::getStatus, 1)))
                .participantCount(ojSubmissionMapper.countDistinctParticipants())
                .build());
    }
}
