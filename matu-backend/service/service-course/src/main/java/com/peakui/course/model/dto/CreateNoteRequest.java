package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNoteRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;
    private Long videoId;
    @NotBlank(message = "笔记内容不能为空")
    @Size(max = 20000, message = "笔记不能超过20000字")
    private String content;
    @Min(0)
    private Integer timestamp;
    @Min(0)
    @Max(1)
    private Integer isPublic;
}
