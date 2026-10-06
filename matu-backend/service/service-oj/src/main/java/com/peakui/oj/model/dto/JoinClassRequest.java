package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "加入班级请求")
public class JoinClassRequest {
    @Schema(description = "邀请码，加入方式为邀请码时使用", example = "ABC12345")
    private String inviteCode;

    @Schema(description = "申请说明", example = "想加入班级学习")
    @NotBlank(message = "申请说明不能为空")
    private String message;
}
