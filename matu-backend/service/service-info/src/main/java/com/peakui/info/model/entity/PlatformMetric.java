package com.peakui.info.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("platform_metrics")
public class PlatformMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private LocalDate metricDate;
    private String metricKey;
    private Long metricValue;
    private String extra;
    private LocalDateTime createdAt;
}
