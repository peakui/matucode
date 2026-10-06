package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageBucketVO {
    private Long id;
    private String bucketName;
    private Integer bucketType;
    private String storageProvider;
    private String endpoint;
    private String region;
    private Long maxSize;
    private Long usedSize;
    private Integer fileCount;
    private Integer status;
}
