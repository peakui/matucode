package com.peakui.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginRequest {

    /** 支持用户名或邮箱登录。 */
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 100)
    private String account;

    /** 原始密码。 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 256)
    private String password;
}
