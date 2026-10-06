package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardAuthStatsVO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-auth", contextId = "authDashboardFeignClient")
public interface AuthDashboardFeignClient {

    @GetMapping("/auth/internal/dashboard/stats")
    ApiResponse<DashboardAuthStatsVO> getDashboardStats();
}
