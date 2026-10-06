package com.peakui.info.feign;

import com.peakui.common.result.ApiResponse;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "service-auth", contextId = "authRoleFeignClient")
public interface AuthRoleFeignClient {

    @GetMapping("/auth/internal/users/{userId}/roles")
    ApiResponse<UserRolesResponse> getUserRoles(@PathVariable("userId") Long userId);

    @Data
    class UserRolesResponse {
        private Long userId;
        private List<String> roles;
    }
}
