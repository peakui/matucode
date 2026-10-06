package com.peakui.course.feign.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChunkInitRequest {
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String fileMd5;
    private Integer chunkSize;
    private Integer chunkCount;
    private String bucketName;
    private Integer ownerType;
    private Integer isPublic;
    private LocalDateTime expiresAt;
}
