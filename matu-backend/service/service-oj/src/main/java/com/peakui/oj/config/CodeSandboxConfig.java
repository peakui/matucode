package com.peakui.oj.config;

import com.peakui.oj.judge.sandbox.model.CodeSandboxProperties;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Configuration
public class CodeSandboxConfig {

    @Bean
    @ConfigurationProperties(prefix = "oj.sandbox")
    public CodeSandboxProperties codeSandboxProperties() {
        return new CodeSandboxProperties();
    }

    @Bean
    public Request.Options codeSandboxFeignOptions(CodeSandboxProperties properties) {
        return new Request.Options(
                properties.getConnectTimeout(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout(), TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public RequestInterceptor codeSandboxRequestInterceptor(CodeSandboxProperties properties) {
        return template -> {
            if (StringUtils.hasText(properties.getAuthHeaderName())
                    && StringUtils.hasText(properties.getAuthHeaderValue())) {
                template.header(properties.getAuthHeaderName().trim(), properties.getAuthHeaderValue().trim());
            }
            if (!StringUtils.hasText(properties.getToken())) {
                return;
            }
            String token = properties.getToken().trim();
            if (!token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
                token = "Bearer " + token;
            }
            template.header("Authorization", token);
        };
    }
}
