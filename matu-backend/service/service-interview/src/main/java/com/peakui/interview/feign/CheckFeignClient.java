package com.peakui.interview.feign;

import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-check")
public interface CheckFeignClient {
    @GetMapping("/checks/statistics/days")
    ApiResponse<Days> getDays(@RequestParam("userId") Long userId);
    record Days(Long userId, Integer totalDays) { }
}
