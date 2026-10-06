package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "班级作业信息")
public class ClassAssignmentVO {
    @Schema(description = "作业ID", example = "1")
    private Long id;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "作业标题", example = "第一周作业")
    private String title;

    @Schema(description = "作业描述", example = "完成基础算法题")
    private String description;

    @Schema(description = "作业类型", example = "1")
    private Integer type;

    @Schema(description = "题目ID列表")
    private List<Long> problemIds;

    @Schema(description = "开始时间", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "截止时间", example = "2026-05-01T23:59:59")
    private LocalDateTime deadline;

    @Schema(description = "最大提交次数", example = "10")
    private Integer maxAttempts;

    @Schema(description = "是否公开榜单 0-否 1-是", example = "1")
    private Integer isPublicRank;

    @Schema(description = "作业状态", example = "1")
    private Integer status;

    @Schema(description = "创建人ID", example = "1001")
    private Long createdBy;

    @Schema(description = "创建时间", example = "2026-04-27T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-04-27T12:00:00")
    private LocalDateTime updatedAt;
}
