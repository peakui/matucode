package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("courses")
public class Course {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long instructorId;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer level;
    private String language;
    private Integer studentCount;
    private Integer chapterCount;
    private Integer videoCount;
    private Integer totalDuration;
    private BigDecimal rating;
    private Integer ratingCount;
    private Integer status;
    private Integer isFree;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
