package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡文章记录表。
 */
@Data
@TableName("check_records")
public class CheckRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String title;
    private String summary;
    private String content;
    private String imageUrls;
    private BigDecimal learnHours;
    private Integer mood;
    private String location;
    private String ipAddress;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer isTop;
    private Integer isFeatured;
    private Integer status;
    private LocalDate checkDate;
    private LocalDateTime checkTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean liked;
}
