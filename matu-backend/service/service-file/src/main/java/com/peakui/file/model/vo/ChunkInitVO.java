package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkInitVO {
    private Long fileId;
    private String uploadId;
    private Integer chunkCount;
    private Integer chunkSize;
    private List<Integer> uploadedChunks;
    private boolean completed;
    private String fileUrl;
}
