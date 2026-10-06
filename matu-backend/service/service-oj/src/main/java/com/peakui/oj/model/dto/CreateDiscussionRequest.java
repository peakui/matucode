package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建讨论请求")
public class CreateDiscussionRequest {
    @Schema(description = "关联作业ID", example = "1")
    private Long assignmentId;

    @Schema(description = "讨论标题", example = "这题为什么超时？")
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "讨论内容", example = "我用双重循环会超时，请问怎么优化？")
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "是否匿名 0-否 1-是", example = "0")
    private Integer isAnonymous;
}
