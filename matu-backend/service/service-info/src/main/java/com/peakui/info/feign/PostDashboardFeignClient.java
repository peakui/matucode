package com.peakui.info.feign;

import com.peakui.common.dashboard.DashboardPostStatsVO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "service-post")
public interface PostDashboardFeignClient {

    @GetMapping("/posts/internal/dashboard/stats")
    ApiResponse<DashboardPostStatsVO> getDashboardStats();
}
