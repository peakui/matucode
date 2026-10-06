package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "作业详情（含题目与当前用户进度）")
public class AssignmentDetailVO {

    @Schema(description = "作业ID", example = "1")
    private Long id;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "作业标题", example = "第一周作业")
    private String title;

    @Schema(description = "作业描述", example = "完成基础算法题")
    private String description;

    @Schema(description = "作业类型 1-OJ算法 2-SQL闯关 3-问答题/项目", example = "1")
    private Integer type;

    @Schema(description = "开始时间", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "截止时间", example = "2026-05-01T23:59:59")
    private LocalDateTime deadline;

    @Schema(description = "最大提交次数 -1表示不限制", example = "10")
    private Integer maxAttempts;

    @Schema(description = "是否公开榜单 0-否 1-是", example = "1")
    private Integer isPublicRank;

    @Schema(description = "作业状态 0-未开始 1-进行中 2-已结束 3-已批改", example = "1")
    private Integer status;

    @Schema(description = "当前用户是否可提交（在时间窗内）", example = "true")
    private Boolean submittable;

    @Schema(description = "题目列表（含当前用户进度）")
    private List<AssignmentProblemVO> problems;
}
