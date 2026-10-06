package com.peakui.qa.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建问题请求。
 */
@Data
@Schema(description = "创建问题请求")
public class CreateQuestionRequest {

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @NotBlank(message = "问题标题不能为空")
    @Size(max = 200, message = "问题标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "问题内容不能为空")
    private String content;

    @Min(value = 0, message = "悬赏积分不能小于0")
    private Integer bountyPoints;
}
