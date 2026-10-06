package com.peakui.file.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileUploadRequest {
    @Min(1)
    @Max(2)
    private Integer ownerType;

    @Min(0)
    @Max(1)
    private Integer isPublic;

    private String bucketName;
    private LocalDateTime expiresAt;

    /**
     * 客户端预计算的文件 MD5，用于图片秒传；服务端仍会在未提供时补算。
     */
    private String fileMd5;
}
