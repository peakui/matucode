package com.peakui.course.feign.vo;

import lombok.Data;

@Data
public class FileSignedUrlVO {
    private Long fileId;
    private String signedUrl;
    private Long expireSeconds;
}
