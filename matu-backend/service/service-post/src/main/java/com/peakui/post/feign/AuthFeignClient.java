package com.peakui.post.feign;

import com.peakui.common.result.ApiResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "service-auth")
public interface AuthFeignClient {

    @GetMapping("/auth/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getUserProfile(@PathVariable("userId") Long userId);

    @GetMapping("/auth/internal/users/{userId}/roles")
    ApiResponse<UserRolesResponse> getUserRoles(@PathVariable("userId") Long userId);

    @PostMapping("/auth/internal/users/{userId}/activity-level/increase")
    ApiResponse<Void> increaseActivityLevel(@PathVariable("userId") Long userId);

    @Data
    class UserProfileResponse {
        private Long userId;
        private String username;
        private Integer status;
        private java.time.LocalDateTime deletedAt;
        private String nickname;
        private String avatarUrl;
        private String email;
        private String schoolName;
        private String companyName;
        private String title;
        private Integer isVip;
        private java.time.LocalDateTime vipExpiredAt;
    }

    @Data
    class UserRolesResponse {
        private Long userId;
        private List<String> roles;
    }
}
