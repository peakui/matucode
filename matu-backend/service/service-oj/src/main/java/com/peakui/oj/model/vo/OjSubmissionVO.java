package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "OJ提交记录信息")
public class OjSubmissionVO {
    @Schema(description = "提交ID", example = "101")
    private Long id;

    @Schema(description = "题目ID", example = "1")
    private Long problemId;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String avatarUrl;

    @Schema(description = "编程语言", example = "java")
    private String language;

    @Schema(description = "判题状态", example = "1")
    private Integer status;

    @Schema(description = "执行耗时 ms", example = "1")
    private Integer executionTime;

    @Schema(description = "内存占用 MB", example = "128")
    private Integer memoryUsed;

    @Schema(description = "通过率", example = "100.00")
    private BigDecimal passRate;

    @Schema(description = "通过用例数", example = "3")
    private Integer passedCases;

    @Schema(description = "总用例数", example = "3")
    private Integer totalCases;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "判题时间", example = "2026-04-27T11:00:00")
    private LocalDateTime judgeTime;

    @Schema(description = "提交时间", example = "2026-04-27T11:00:00")
    private LocalDateTime createdAt;
}
