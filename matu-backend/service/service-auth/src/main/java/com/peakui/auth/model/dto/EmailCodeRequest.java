package com.peakui.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 邮箱验证码发送请求。
 */
@Data
@Schema(description = "发送注册邮箱验证码请求")
public class EmailCodeRequest {

    /** 邮箱。 */
    @Schema(description = "注册邮箱", example = "demo@qq.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
