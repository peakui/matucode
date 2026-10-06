package com.peakui.pay.feign;

import com.peakui.common.result.ApiResponse;
import com.peakui.pay.model.dto.ActivateVipRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "service-auth", path = "/auth/internal")
public interface AuthFeignClient {
    @PostMapping("/users/{userId}/vip/activate")
    ApiResponse<Void> activateVip(@PathVariable("userId") Long userId, @RequestBody ActivateVipRequest request);
}
