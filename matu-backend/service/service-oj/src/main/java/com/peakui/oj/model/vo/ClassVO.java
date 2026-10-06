package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "班级信息")
public class ClassVO {
    @Schema(description = "班级ID", example = "1")
    private Long id;

    @Schema(description = "班级名称", example = "Java刷题班")
    private String name;

    @Schema(description = "班级类型 1-普通班级 2-竞赛", example = "1")
    private Integer type;

    @Schema(description = "班级描述", example = "2026春季训练营")
    private String description;

    @Schema(description = "创建人ID", example = "1001")
    private Long creatorId;

    @Schema(description = "封面图", example = "https://xxx.com/cover.png")
    private String coverImage;

    @Schema(description = "加入方式 1-公开加入 2-审核加入 3-邀请码加入", example = "1")
    private Integer joinMode;

    @Schema(description = "邀请码", example = "ABC12345")
    private String inviteCode;

    @Schema(description = "班级状态", example = "1")
    private Integer status;

    @Schema(description = "当前用户是否已加入", example = "true")
    private Boolean joined;

    @Schema(description = "成员数量", example = "35")
    private Integer memberCount;

    @Schema(description = "开始时间（竞赛用）", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间（竞赛用）", example = "2026-04-27T12:00:00")
    private LocalDateTime endTime;

    @Schema(description = "创建时间", example = "2026-04-27T10:00:00")
    private LocalDateTime createdAt;
}
