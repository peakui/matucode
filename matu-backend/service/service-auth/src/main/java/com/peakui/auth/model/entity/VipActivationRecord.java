package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vip_activation_records")
public class VipActivationRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activation_key")
    private String activationKey;

    @TableField("user_id")
    private Long userId;

    private Integer days;
    private Integer level;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
