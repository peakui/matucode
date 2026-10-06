package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户打卡统计表。
 */
@Data
@TableName("check_statistics")
public class CheckStatistic {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Integer totalDays;
    private Integer continuousDays;
    private Integer maxContinuousDays;
    private Integer totalArticles;
    private Integer totalLikesReceived;
    private BigDecimal totalLearnHours;
    private LocalDate lastCheckDate;
    private Integer checkYear;
    private Integer checkMonth;
    private LocalDateTime updatedAt;
}
