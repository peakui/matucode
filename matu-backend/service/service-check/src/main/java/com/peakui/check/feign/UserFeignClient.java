package com.peakui.check.feign;

import com.peakui.check.feign.vo.UserProfileVO;
import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 用户信息远程调用。
 */
@FeignClient(name = "service-auth", path = "/auth/internal/users")
public interface UserFeignClient {

    @GetMapping("/{userId}")
    ApiResponse<UserProfileVO> getUserProfile(@PathVariable("userId") Long userId);

    @PostMapping("/{userId}/activity-level/increase")
    ApiResponse<Void> increaseActivityLevel(@PathVariable("userId") Long userId);
}
