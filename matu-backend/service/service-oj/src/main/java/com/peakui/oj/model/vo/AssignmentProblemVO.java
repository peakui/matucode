package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "作业内题目及其对当前用户的进度")
public class AssignmentProblemVO {

    @Schema(description = "题目ID", example = "1001")
    private Long problemId;

    @Schema(description = "题号", example = "A+B")
    private String problemNo;

    @Schema(description = "题目标题", example = "两数之和")
    private String title;

    @Schema(description = "难度 1-简单 2-中等 3-困难", example = "1")
    private Integer difficulty;

    @Schema(description = "当前用户是否已通过", example = "false")
    private Boolean solved;

    @Schema(description = "当前用户已提交次数", example = "2")
    private Integer attemptsUsed;

    @Schema(description = "最近一次提交ID", example = "101")
    private Long latestSubmissionId;
}
