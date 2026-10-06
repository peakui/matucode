package com.peakui.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供内部服务调用的用户信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private String signature;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String schoolName;
    private String companyName;
    private String title;
    private Integer isVip;
    private Integer vipLevel;
    private LocalDateTime vipExpiredAt;
    private Integer vipDaysRemaining;
}
