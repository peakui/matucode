package com.peakui.file.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkStatusVO {
    private Long fileId;
    private String uploadId;
    private Integer chunkCount;
    private List<Integer> uploadedChunks;
    private boolean completed;
}
