package com.peakui.info.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/** 服务间调用面试统计接口的独立凭证，不复用用户身份头。 */
public class InterviewDashboardFeignConfig {
    @Value("${security.internal.service-secret:}")
    private String serviceSecret;

    @Bean
    public RequestInterceptor interviewDashboardRequestInterceptor() {
        return template -> template.header("X-Internal-Service-Secret", serviceSecret);
    }
}
