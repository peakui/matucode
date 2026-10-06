package com.peakui.oj.feign;

import com.peakui.common.result.ApiResponse;
import com.peakui.oj.feign.vo.UserBriefVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "service-auth")
public interface AuthFeignClient {

    @GetMapping("/auth/internal/users/batch")
    ApiResponse<List<UserBriefVO>> getUserBriefs(@RequestParam("ids") List<Long> ids);
}
