package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "OJ测试用例信息")
public class OjTestCaseVO {
    @Schema(description = "用例ID", example = "1")
    private Long id;

    @Schema(description = "题目ID", example = "1")
    private Long problemId;

    @Schema(description = "用例编号", example = "1")
    private Integer caseNo;

    @Schema(description = "输入内容")
    private String input;

    @Schema(description = "期望输出")
    private String expectedOutput;

    @Schema(description = "是否样例 0-否 1-是", example = "1")
    private Integer isSample;

    @Schema(description = "分值权重", example = "1.00")
    private java.math.BigDecimal scoreWeight;

    @Schema(description = "是否隐藏 0-否 1-是", example = "0")
    private Integer isHidden;

    @Schema(description = "创建时间", example = "2026-04-27T10:00:00")
    private LocalDateTime createdAt;
}
