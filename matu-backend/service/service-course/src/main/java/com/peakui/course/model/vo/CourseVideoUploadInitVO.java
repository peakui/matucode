package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVideoUploadInitVO {
    private Long fileId;
    private String uploadId;
    private Integer chunkCount;
    private Integer chunkSize;
    private List<Integer> uploadedChunks;
    private String chunkUploadUrl;
    private String chunkStatusUrl;
    private String completeUrl;
}
