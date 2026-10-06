package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileSignedUrlVO {
    private Long fileId;
    private String signedUrl;
    private Long expireSeconds;
}
