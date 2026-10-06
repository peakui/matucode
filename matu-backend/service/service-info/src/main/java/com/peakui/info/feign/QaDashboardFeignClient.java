package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardQaStatsVO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-qa")
public interface QaDashboardFeignClient {

    @GetMapping("/qa/internal/dashboard/stats")
    ApiResponse<DashboardQaStatsVO> getDashboardStats();
}
