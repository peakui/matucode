package com.peakui.file.model.dto;

import lombok.Data;

@Data
public class FileQueryRequest {
    private Long ownerId;
    private Integer ownerType;
    private String fileType;
    private String bucketName;
    private Integer status;
    private Long pageNum;
    private Long pageSize;
}
