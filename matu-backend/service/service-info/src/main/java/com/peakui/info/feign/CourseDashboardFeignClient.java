package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardCourseStatsDTO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-course")
public interface CourseDashboardFeignClient {

    @GetMapping("/courses/internal/dashboard/stats")
    ApiResponse<DashboardCourseStatsDTO> getDashboardStats();
}
