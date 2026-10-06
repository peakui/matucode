package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新OJ题目请求")
public class UpdateOjProblemRequest {
    @Schema(description = "题目编号", example = "OJ1001")
    private String problemNo;

    @Schema(description = "题目标题", example = "A + B Problem")
    @NotBlank(message = "题目标题不能为空")
    private String title;

    @Schema(description = "题目描述", example = "输入两个整数，输出它们的和。")
    @NotBlank(message = "题目描述不能为空")
    private String description;

    @Schema(description = "输入格式", example = "一行两个整数")
    private String inputFormat;

    @Schema(description = "输出格式", example = "输出一个整数")
    private String outputFormat;

    @Schema(description = "样例输入", example = "1 2")
    private String sampleInput;

    @Schema(description = "样例输出", example = "3")
    private String sampleOutput;

    @Schema(description = "提示信息", example = "注意整数范围")
    private String hint;

    @Schema(description = "难度", example = "1")
    private Integer difficulty;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "时间限制 ms", example = "1000")
    private Integer timeLimit;

    @Schema(description = "内存限制 MB", example = "256")
    private Integer memoryLimit;

    @Schema(description = "题目状态", example = "1")
    private Integer status;

    @Schema(description = "Markdown题面")
    private String contentMd;

    @Schema(description = "HTML题面")
    private String contentHtml;

    @Schema(description = "起始代码JSON")
    private String starterCodeJson;

    @Schema(description = "题解JSON")
    private String solutionJson;

    @Schema(description = "标签列表")
    private List<String> tags;
}
