package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_external_identities")
public class ExternalIdentity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String provider;
    private String appId;
    private String openId;
    private String unionId;
    private LocalDateTime createdAt;
}
