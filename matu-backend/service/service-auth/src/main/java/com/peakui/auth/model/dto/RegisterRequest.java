package com.peakui.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求参数。
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    /** 用户名。 */
    @Schema(description = "用户名/登录账号", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在 4-20 位之间")
    private String username;

    /** 用户昵称。 */
    @Schema(description = "用户昵称，不传时系统自动生成，格式如：用户1A2B", example = "测试用户")
    @Size(max = 50, message = "用户昵称长度不能超过 50 位")
    private String nickname;

    /** 邮箱。 */
    @Schema(description = "注册邮箱", example = "demo@qq.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 邮箱验证码。 */
    @Schema(description = "邮箱验证码，需先调用发送验证码接口获取", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "邮箱验证码必须为 6 位")
    private String emailCode;

    /** 手机号。 */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /** 原始密码。 */
    @Schema(description = "登录密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需在 6-20 位之间")
    private String password;

    /** 确认密码。 */
    @Schema(description = "确认密码，需要与 password 一致", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
