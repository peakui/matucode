package com.peakui.course.feign.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChunkInitVO {
    private Long fileId;
    private String uploadId;
    private Integer chunkCount;
    private Integer chunkSize;
    private List<Integer> uploadedChunks;
}
