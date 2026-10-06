package com.peakui.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件服务配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    private long imageMaxSizeBytes;
    private long fileMaxSizeBytes;
    private String imageAllowedExtensions;
    private String fileAllowedExtensions;
    private long signedUrlExpireSeconds;
    private OssProperties oss = new OssProperties();

    public List<String> imageExtensionList() {
        return splitToList(imageAllowedExtensions);
    }

    public List<String> fileExtensionList() {
        return splitToList(fileAllowedExtensions);
    }

    private List<String> splitToList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @Data
    public static class OssProperties {
        private String endpoint;
        private String region;
        private String bucketName;
        private String bucketDomain;
        private String publicDomain;
        private String accessKeyId;
        private String accessKeySecret;
        private boolean privateBucket = true;
    }
}
