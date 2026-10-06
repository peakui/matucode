package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就定义表。
 */
@Data
@TableName("achievements")
public class Achievement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String achievementCode;
    private String achievementName;
    private String achievementDesc;
    private String achievementIcon;
    private Integer achievementType;
    private Integer conditionType;
    private Integer conditionValue;
    private Integer pointReward;
    private Integer status;
    private LocalDateTime createdAt;
}
