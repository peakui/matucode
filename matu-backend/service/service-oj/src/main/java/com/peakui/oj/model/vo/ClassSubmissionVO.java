package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "班级作业提交记录")
public class ClassSubmissionVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "作业ID", example = "1")
    private Long assignmentId;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "昵称", example = "Alice")
    private String nickname;

    @Schema(description = "题目ID", example = "1001")
    private Long problemId;

    @Schema(description = "题目标题", example = "两数之和")
    private String problemTitle;

    @Schema(description = "关联判题提交ID", example = "101")
    private Long submissionId;

    @Schema(description = "状态 0-未完成 1-已完成 2-超时未交", example = "1")
    private Integer status;

    @Schema(description = "得分", example = "100.00")
    private BigDecimal score;

    @Schema(description = "提交时间", example = "2026-04-27T10:00:00")
    private LocalDateTime submittedAt;
}
