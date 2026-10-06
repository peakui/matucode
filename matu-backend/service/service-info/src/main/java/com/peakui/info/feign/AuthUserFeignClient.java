package com.peakui.info.feign;

import com.peakui.common.result.ApiResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-auth", contextId = "authUserFeignClient")
public interface AuthUserFeignClient {

    @GetMapping("/auth/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getUserProfile(@PathVariable("userId") Long userId);

    @Data
    class UserProfileResponse {
        private Long userId;
        private String username;
        private String nickname;
        private String avatarUrl;
        private String email;
    }
}
