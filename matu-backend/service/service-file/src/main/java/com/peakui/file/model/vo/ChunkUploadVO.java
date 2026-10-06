package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkUploadVO {
    private Long fileId;
    private Integer chunkNo;
    private Integer uploadStatus;
}
