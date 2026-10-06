package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建OJ测试用例请求")
public class CreateOjTestCaseRequest {
    @Schema(description = "用例编号", example = "1")
    @NotNull(message = "用例编号不能为空")
    private Integer caseNo;

    @Schema(description = "输入内容", example = "1 2")
    @NotBlank(message = "输入不能为空")
    private String input;

    @Schema(description = "期望输出", example = "3")
    @NotBlank(message = "期望输出不能为空")
    private String expectedOutput;

    @Schema(description = "是否样例 0-否 1-是", example = "1")
    private Integer isSample;

    @Schema(description = "分值权重", example = "1.00")
    private BigDecimal scoreWeight;

    @Schema(description = "是否隐藏 0-否 1-是", example = "0")
    private Integer isHidden;
}
