package com.peakui.course.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCourseRequest {
    private Long categoryId;
    private String title;
    private String subtitle;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer level;
    private String language;
    private Integer isFree;
    private Integer status;
}
