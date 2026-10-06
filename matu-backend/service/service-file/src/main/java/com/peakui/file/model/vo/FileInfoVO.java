package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FileInfoVO {
    private Long id;
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String fileMd5;
    private String bucketName;
    private Long ownerId;
    private Integer ownerType;
    private Integer isPublic;
    private Integer downloadCount;
    private Integer status;
    private String uploadId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
