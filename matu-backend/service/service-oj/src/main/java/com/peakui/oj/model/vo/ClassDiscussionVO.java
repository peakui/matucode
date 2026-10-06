package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "班级讨论信息")
public class ClassDiscussionVO {
    @Schema(description = "讨论ID", example = "1")
    private Long id;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "作业ID", example = "1")
    private Long assignmentId;

    @Schema(description = "发帖用户ID", example = "1001")
    private Long userId;

    @Schema(description = "讨论标题", example = "这题为什么超时？")
    private String title;

    @Schema(description = "讨论内容")
    private String content;

    @Schema(description = "是否匿名 0-否 1-是", example = "0")
    private Integer isAnonymous;

    @Schema(description = "创建时间", example = "2026-04-27T10:00:00")
    private LocalDateTime createdAt;
}
