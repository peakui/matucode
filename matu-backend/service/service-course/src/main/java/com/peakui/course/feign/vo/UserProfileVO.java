package com.peakui.course.feign.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
}
