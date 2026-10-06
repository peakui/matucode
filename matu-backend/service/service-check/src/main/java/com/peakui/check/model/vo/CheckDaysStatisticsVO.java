package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户打卡天数统计")
public class CheckDaysStatisticsVO {

    private Long userId;
    private Integer totalDays;
}
