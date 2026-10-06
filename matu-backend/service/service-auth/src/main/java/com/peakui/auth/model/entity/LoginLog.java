package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志表实体。
 */
@Data
@TableName("login_logs")
public class LoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("login_type")
    private Integer loginType;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_location")
    private String loginLocation;

    @TableField("device_type")
    private String deviceType;

    private String browser;

    private String os;

    @TableField("login_status")
    private Integer loginStatus;

    @TableField("fail_reason")
    private String failReason;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
