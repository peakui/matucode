package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就表。
 */
@Data
@TableName("user_achievements")
public class UserAchievement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long achievementId;
    private LocalDateTime achievedAt;
    private Integer progress;
    private Integer isClaimed;
}
