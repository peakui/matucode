package com.peakui.interview.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FeignRequestHeaderConfig {
    @Bean
    public RequestInterceptor vipServiceAuthentication(
            @Value("${security.vip.service-token:}") String token) {
        return template -> {
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("未配置会员服务调用凭证");
            }
            template.header("X-Vip-Service-Token", token);
        };
    }
}
