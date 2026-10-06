package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "提交OJ代码请求")
public class SubmitOjCodeRequest {
    @Schema(description = "题目ID", example = "1")
    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    @Schema(description = "班级作业ID，自由刷题时为空", example = "1")
    private Long assignmentId;

    @Schema(description = "编程语言", example = "java")
    @NotBlank(message = "编程语言不能为空")
    private String language;

    @Schema(description = "提交代码")
    @NotBlank(message = "代码不能为空")
    private String code;
}
