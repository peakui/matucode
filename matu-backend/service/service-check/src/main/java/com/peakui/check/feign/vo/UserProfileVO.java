package com.peakui.check.feign.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 远程用户信息。
 */
@Data
public class UserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String schoolName;
    private String companyName;
    private String title;
    private Integer isVip;
    private LocalDateTime vipExpiredAt;
}
