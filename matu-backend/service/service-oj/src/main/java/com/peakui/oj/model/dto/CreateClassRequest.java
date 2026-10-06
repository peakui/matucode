package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建班级请求")
public class CreateClassRequest {
    @Schema(description = "班级名称", example = "Java刷题班")
    @NotBlank(message = "班级名称不能为空")
    private String name;

    @Schema(description = "班级类型 1-普通班级 2-竞赛", example = "1")
    @NotNull(message = "班级类型不能为空")
    private Integer type;

    @Schema(description = "班级描述", example = "2026春季训练营")
    private String description;

    @Schema(description = "封面图片地址", example = "https://xxx.com/cover.png")
    private String coverImage;

    @Schema(description = "加入方式 1-公开加入 2-审核加入 3-邀请码加入", example = "1")
    @NotNull(message = "加入方式不能为空")
    private Integer joinMode;

    @Schema(description = "邀请码，加入方式为3时可传", example = "ABC12345")
    private String inviteCode;

    @Schema(description = "开始时间（竞赛用）", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间（竞赛用）", example = "2026-04-27T12:00:00")
    private LocalDateTime endTime;
}
