package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户成就返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户成就")
public class UserAchievementVO {

    private Long id;
    private Long achievementId;
    private String achievementCode;
    private String achievementName;
    private String achievementDesc;
    private String achievementIcon;
    private Integer achievementType;
    private Integer conditionType;
    private Integer conditionValue;
    private Integer pointReward;
    private Integer progress;
    private Integer isClaimed;
    private LocalDateTime achievedAt;
}
