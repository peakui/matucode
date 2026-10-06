package com.peakui.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录/注册成功后的返回体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenVO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String signature;
    private List<String> roles;
    private String tokenName;
    private String tokenValue;
    private String authorization;
    private long tokenTimeout;
    private LocalDateTime lastLoginTime;
}
