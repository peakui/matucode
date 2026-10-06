package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "OJ提交测试点详情")
public class OjSubmissionDetailVO {
    @Schema(description = "明细ID", example = "1")
    private Long id;

    @Schema(description = "提交ID", example = "101")
    private Long submissionId;

    @Schema(description = "测试点编号", example = "1")
    private Integer caseNo;

    @Schema(description = "输入内容")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "实际输出")
    private String actualOutput;

    @Schema(description = "判题状态", example = "1")
    private Integer status;

    @Schema(description = "执行耗时 ms", example = "1")
    private Integer executionTime;

    @Schema(description = "内存占用 MB", example = "128")
    private Integer memoryUsed;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间", example = "2026-04-27T11:00:01")
    private LocalDateTime createdAt;
}
