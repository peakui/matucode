package com.peakui.file.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * OSS 配置。
 */
@Configuration
public class OssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(FileProperties fileProperties) {
        FileProperties.OssProperties oss = fileProperties.getOss();
        if (!StringUtils.hasText(oss.getEndpoint())
                || !StringUtils.hasText(oss.getAccessKeyId())
                || !StringUtils.hasText(oss.getAccessKeySecret())) {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("shutdown".equals(method.getName())) {
                    return null;
                }
                throw new IllegalStateException("OSS 未配置");
            };
            return (OSS) Proxy.newProxyInstance(OSS.class.getClassLoader(), new Class<?>[]{OSS.class}, handler);
        }
        return new OSSClientBuilder().build(normalizeEndpoint(oss.getEndpoint()), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }

    // A scheme-less endpoint makes the SDK default to plain HTTP, which some
    // networks (campus) reset mid-connection (WSAECONNABORTED). Force HTTPS.
    private String normalizeEndpoint(String endpoint) {
        String trimmed = endpoint.trim();
        return trimmed.contains("://") ? trimmed : "https://" + trimmed;
    }
}
