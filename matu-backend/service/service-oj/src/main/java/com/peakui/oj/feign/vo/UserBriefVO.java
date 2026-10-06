package com.peakui.oj.feign.vo;

import lombok.Data;

/** service-oj 侧接收的用户简要信息，字段与 service-auth 的 UserBriefVO 保持一致。 */
@Data
public class UserBriefVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
}
