package com.peakui.interview.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建面试题请求")
public class CreateInterviewQuestionRequest {
    @Schema(description = "题目编号", example = "INT1001")
    @NotBlank(message = "题目编号不能为空")
    private String questionNo;

    @Schema(description = "题目名称", example = "Java 中 HashMap 的底层实现")
    @NotBlank(message = "题目名称不能为空")
    private String title;

    @Schema(description = "题目内容")
    @NotBlank(message = "题目内容不能为空")
    private String content;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "来源公司ID", example = "1")
    private Long companyId;

    @Schema(description = "岗位标签")
    private List<String> positionTags;

    @Schema(description = "难度 1简单 2中等 3困难", example = "2")
    @NotNull(message = "难度不能为空")
    private Integer difficulty;

    @Schema(description = "是否锁定 0否 1是", example = "0")
    private Integer isLocked;

    @Schema(description = "解锁所需打卡天数", example = "0")
    private Integer unlockDays;

    @Schema(description = "状态", example = "1")
    private Integer status;
}
