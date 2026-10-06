package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteCourseVideoUploadRequest {
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
    @NotBlank(message = "视频标题不能为空")
    private String videoTitle;
    private String videoDesc;
    private String coverUrl;
    private String resolution;
    private Integer sortOrder;
    private Integer isFreePreview;
}
