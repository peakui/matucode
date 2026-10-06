package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "OJ题目信息")
public class OjProblemVO {
    @Schema(description = "题目ID", example = "1")
    private Long id;

    @Schema(description = "题目编号", example = "OJ1001")
    private String problemNo;

    @Schema(description = "题目标题", example = "A + B Problem")
    private String title;

    @Schema(description = "题目描述")
    private String description;

    @Schema(description = "输入格式")
    private String inputFormat;

    @Schema(description = "输出格式")
    private String outputFormat;

    @Schema(description = "样例输入")
    private String sampleInput;

    @Schema(description = "样例输出")
    private String sampleOutput;

    @Schema(description = "提示信息")
    private String hint;

    @Schema(description = "难度", example = "1")
    private Integer difficulty;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "时间限制 ms", example = "1000")
    private Integer timeLimit;

    @Schema(description = "内存限制 MB", example = "256")
    private Integer memoryLimit;

    @Schema(description = "提交次数", example = "20")
    private Integer submitCount;

    @Schema(description = "通过次数", example = "8")
    private Integer acceptCount;

    @Schema(description = "通过率", example = "40.00")
    private BigDecimal acceptRate;

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

    @Schema(description = "创建时间", example = "2026-04-27T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-04-27T12:00:00")
    private LocalDateTime updatedAt;
}
