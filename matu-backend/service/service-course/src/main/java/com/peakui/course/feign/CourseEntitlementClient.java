package com.peakui.course.feign;

import com.peakui.common.result.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="service-auth",contextId="courseEntitlement",path="/auth")
public interface CourseEntitlementClient {
    @GetMapping("/me/vip") ApiResponse<VipAccess> vip(@RequestHeader("Authorization") String authorization);
    record VipAccess(Boolean valid) {}
}
