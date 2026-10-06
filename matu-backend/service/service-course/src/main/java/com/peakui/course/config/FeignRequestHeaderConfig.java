package com.peakui.course.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class FeignRequestHeaderConfig {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    private final HttpServletRequest request;

    @Bean
    public RequestInterceptor requestHeaderRelayInterceptor() {
        return template -> {
            relayHeader(template, HEADER_USER_ID);
            relayHeader(template, HEADER_USER_ROLES);
            relayHeader(template, HttpHeaders.AUTHORIZATION);
        };
    }

    private void relayHeader(feign.RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (StringUtils.hasText(value)) {
            template.header(headerName, value);
        }
    }
}
