package com.peakui.interview.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建面试答案请求")
public class CreateInterviewAnswerRequest {
    @Schema(description = "答案内容")
    @NotBlank(message = "答案内容不能为空")
    private String content;

    @Schema(description = "答案类型 1官方 2用户", example = "2")
    private Integer answerType;

    @Schema(description = "内容类型 1文字 2代码 3混合", example = "1")
    private Integer contentType;

    @Schema(description = "代码片段")
    private String codeSnippet;

    @Schema(description = "是否官方答案 0否 1是", example = "0")
    private Integer isOfficial;

    @Schema(description = "是否锁定 0否 1是", example = "0")
    private Integer isLocked;

    @Schema(description = "解锁所需打卡天数", example = "0")
    private Integer unlockDays;

    @Schema(description = "状态", example = "1")
    private Integer status;
}
