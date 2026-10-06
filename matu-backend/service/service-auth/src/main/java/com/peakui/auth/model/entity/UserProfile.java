package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户扩展信息表实体。
 */
@Data
@TableName("user_profiles")
public class UserProfile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("activity_level")
    private Long activityLevel;

    @TableField("school_name")
    private String schoolName;

    @TableField("school_verified")
    private Integer schoolVerified;

    @TableField("school_verify_time")
    private LocalDateTime schoolVerifyTime;

    @TableField("company_name")
    private String companyName;

    @TableField("company_verified")
    private Integer companyVerified;

    @TableField("company_verify_time")
    private LocalDateTime companyVerifyTime;

    private String title;

    @TableField("title_verified")
    private Integer titleVerified;

    private String major;

    private String grade;

    @TableField("work_years")
    private Integer workYears;

    @TableField("blog_url")
    private String blogUrl;

    @TableField("github_url")
    private String githubUrl;

    @TableField("wechat_url")
    private String wechatUrl;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("follower_count")
    private Integer followerCount;

    @TableField("following_count")
    private Integer followingCount;

    @TableField("is_vip")
    private Integer isVip;

    @TableField("vip_level")
    private Integer vipLevel;

    @TableField("vip_expired_at")
    private LocalDateTime vipExpiredAt;

    @TableField("vip_days_remaining")
    private Integer vipDaysRemaining;
}
