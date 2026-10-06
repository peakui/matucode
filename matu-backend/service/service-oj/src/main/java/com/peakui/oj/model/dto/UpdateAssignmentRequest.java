package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "更新作业请求")
public class UpdateAssignmentRequest {
    @Schema(description = "作业标题", example = "第一周作业-更新")
    @NotBlank(message = "作业标题不能为空")
    private String title;

    @Schema(description = "作业描述", example = "补充更多基础算法题")
    private String description;

    @Schema(description = "作业类型", example = "1")
    private Integer type;

    @Schema(description = "题目ID列表", example = "[1,2,3]")
    private List<Long> problemIds;

    @Schema(description = "开始时间", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "截止时间", example = "2026-05-01T23:59:59")
    private LocalDateTime deadline;

    @Schema(description = "最大提交次数 -1表示不限制", example = "10")
    private Integer maxAttempts;

    @Schema(description = "是否公开榜单 0-否 1-是", example = "1")
    private Integer isPublicRank;

    @Schema(description = "作业状态", example = "1")
    private Integer status;
}
