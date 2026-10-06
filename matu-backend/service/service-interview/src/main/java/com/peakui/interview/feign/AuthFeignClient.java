package com.peakui.interview.feign;

import com.peakui.common.result.ApiResponse;
import com.peakui.interview.feign.AuthFeignClient.VipStatusVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-auth", path = "/auth/internal")
public interface AuthFeignClient {
    @GetMapping("/users/{userId}/vip")
    ApiResponse<VipStatusVO> getVipStatus(@PathVariable("userId") Long userId);

    record VipStatusVO(Long userId, Boolean valid, Integer isVip, Integer vipLevel,
                       java.time.LocalDateTime vipExpiredAt, Integer vipDaysRemaining) {
    }
}
