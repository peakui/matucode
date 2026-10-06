package com.peakui.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private Integer isFollowed;
    private Integer followerCount;
    private Integer followingCount;
}
