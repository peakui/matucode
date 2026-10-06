package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVideoRequest {
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;
    @NotBlank(message = "视频标题不能为空")
    private String videoTitle;
    private String videoDesc;
    @NotBlank(message = "视频URL不能为空")
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private Integer sortOrder;
    private Integer isFreePreview;
}
