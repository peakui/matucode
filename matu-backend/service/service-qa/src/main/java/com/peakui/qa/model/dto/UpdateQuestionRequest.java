package com.peakui.qa.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新问题请求。
 */
@Data
@Schema(description = "更新问题请求")
public class UpdateQuestionRequest {

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Size(max = 200, message = "问题标题长度不能超过200个字符")
    private String title;

    private String content;

    @Min(value = 0, message = "悬赏积分不能小于0")
    private Integer bountyPoints;

    @Schema(description = "问题状态 0待解决 1已解决 2已关闭", example = "0")
    private Integer status;
}
