package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "更新班级请求")
public class UpdateClassRequest {
    @Schema(description = "班级名称", example = "Java刷题班-升级版")
    @NotBlank(message = "班级名称不能为空")
    private String name;

    @Schema(description = "班级类型 1-普通班级 2-竞赛", example = "2")
    private Integer type;

    @Schema(description = "班级描述", example = "班级描述更新")
    private String description;

    @Schema(description = "封面图片地址", example = "https://xxx.com/new-cover.png")
    private String coverImage;

    @Schema(description = "加入方式 1-公开加入 2-审核加入 3-邀请码加入", example = "3")
    private Integer joinMode;

    @Schema(description = "邀请码", example = "ABC12345")
    private String inviteCode;

    @Schema(description = "班级状态", example = "1")
    private Integer status;

    @Schema(description = "开始时间（竞赛用）", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间（竞赛用）", example = "2026-04-27T12:00:00")
    private LocalDateTime endTime;
}
