package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChapterRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    @NotBlank(message = "章节标题不能为空")
    private String chapterTitle;
    private String chapterDesc;
    private Integer sortOrder;
    private Integer isFreePreview;
}
