package com.peakui.interview.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建模拟面试请求")
public class CreateMockInterviewRequest {
    @Schema(description = "模拟面试标题", example = "Java 后端一面")
    @NotBlank(message = "模拟面试标题不能为空")
    private String interviewTitle;

    @Schema(description = "岗位", example = "Java后端")
    private String position;

    @Schema(description = "公司", example = "阿里")
    private String company;

    @Schema(description = "题目ID列表")
    private List<Long> questionIds;

    @Schema(description = "时长(分钟)", example = "30")
    private Integer durationMinutes;
}
