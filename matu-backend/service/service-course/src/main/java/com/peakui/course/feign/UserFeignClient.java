package com.peakui.course.feign;

import com.peakui.common.result.ApiResponse;
import com.peakui.course.feign.vo.UserProfileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-auth", path = "/auth/internal/users")
public interface UserFeignClient {

    @GetMapping("/{userId}")
    ApiResponse<UserProfileVO> getUserProfile(@PathVariable("userId") Long userId);

    @GetMapping("/by-username/{username}")
    ApiResponse<UserProfileVO> getUserProfileByUsername(@PathVariable("username") String username);
}
