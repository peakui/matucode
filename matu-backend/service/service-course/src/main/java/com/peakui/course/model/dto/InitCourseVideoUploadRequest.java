package com.peakui.course.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitCourseVideoUploadRequest {
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;
    @NotBlank(message = "文件名不能为空")
    private String originalName;
    private String fileType;
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;
    private String fileMd5;
    @NotNull(message = "分片大小不能为空")
    @Min(value = 1, message = "分片大小必须大于0")
    private Integer chunkSize;
    @NotNull(message = "分片数量不能为空")
    @Min(value = 1, message = "分片数量必须大于0")
    private Integer chunkCount;
    private String bucketName;
}
