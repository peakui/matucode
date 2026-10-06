package com.peakui.check.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成就领奖请求。
 */
@Data
@Schema(description = "成就领奖请求")
public class ClaimAchievementRewardRequest {

    @NotNull(message = "成就ID不能为空")
    @Schema(description = "成就ID", example = "1")
    private Long achievementId;
}
