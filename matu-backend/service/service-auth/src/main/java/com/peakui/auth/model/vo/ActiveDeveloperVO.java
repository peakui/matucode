package com.peakui.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动开发者展示信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveDeveloperVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Long activityLevel;
}
