package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "班级成员信息")
public class ClassMemberVO {
    @Schema(description = "成员记录ID", example = "1")
    private Long id;

    @Schema(description = "班级ID", example = "1")
    private Long classId;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "昵称", example = "Alice")
    private String nickname;

    @Schema(description = "头像", example = "https://xxx.com/a.png")
    private String avatarUrl;

    @Schema(description = "角色 1-教师 2-助教 3-学生", example = "3")
    private Integer role;

    @Schema(description = "加入状态", example = "1")
    private Integer joinStatus;

    @Schema(description = "加入时间", example = "2026-04-27T10:00:00")
    private LocalDateTime joinedAt;
}
