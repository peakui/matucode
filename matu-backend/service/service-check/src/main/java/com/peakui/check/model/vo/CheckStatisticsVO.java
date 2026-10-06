package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 打卡统计返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "打卡统计")
public class CheckStatisticsVO {

    private Long userId;
    private Integer year;
    private Integer month;
    private Integer totalDays;
    private Integer continuousDays;
    private Integer maxContinuousDays;
    private Integer totalArticles;
    private Integer totalLikesReceived;
    private BigDecimal totalLearnHours;
    private LocalDate lastCheckDate;
}
