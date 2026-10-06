package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardCheckStatsVO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-check")
public interface CheckDashboardFeignClient {

    @GetMapping("/checks/internal/dashboard/stats")
    ApiResponse<DashboardCheckStatsVO> getDashboardStats();
}
