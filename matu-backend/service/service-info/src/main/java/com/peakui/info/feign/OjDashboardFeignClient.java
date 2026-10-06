package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardOjStatsDTO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-oj")
public interface OjDashboardFeignClient {

    @GetMapping("/oj/internal/dashboard/stats")
    ApiResponse<DashboardOjStatsDTO> getDashboardStats();
}
