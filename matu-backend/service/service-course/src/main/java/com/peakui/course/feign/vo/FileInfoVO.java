package com.peakui.course.feign.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
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
    private Integer status;
    private String uploadId;
    private LocalDateTime createdAt;
}
