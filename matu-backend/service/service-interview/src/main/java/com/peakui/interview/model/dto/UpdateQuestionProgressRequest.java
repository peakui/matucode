package com.peakui.interview.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新用户题目进度请求")
public class UpdateQuestionProgressRequest {
    @Schema(description = "状态 0未做 1已做 2掌握 3需复习", example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "用户答案")
    private String answerContent;

    @Schema(description = "掌握程度 1-5", example = "3")
    private Integer masteryLevel;

    @Schema(description = "练习次数增量", example = "1")
    private Integer practiceCount;

    @Schema(description = "错误原因")
    private String wrongReason;

    @Schema(description = "是否加入错题本 0否 1是", example = "0")
    private Integer addToWrongBook;
}
