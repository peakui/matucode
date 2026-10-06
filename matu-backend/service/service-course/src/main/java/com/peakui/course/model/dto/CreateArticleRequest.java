package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateArticleRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private Long chapterId;
    @NotBlank(message = "文章标题不能为空")
    private String title;
    @NotBlank(message = "文章内容不能为空")
    private String content;
    private Integer sortOrder;
    private Integer status;
}
