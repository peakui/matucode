package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardInterviewStatsDTO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-interview", configuration = InterviewDashboardFeignConfig.class)
public interface InterviewDashboardFeignClient {

    @GetMapping("/interview/internal/dashboard/stats")
    ApiResponse<DashboardInterviewStatsDTO> getDashboardStats();
}
