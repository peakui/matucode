package com.peakui.course.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCourseRequest {
    private Long categoryId;
    @NotBlank(message = "课程标题不能为空")
    private String title;
    private String subtitle;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer level;
    private String language;
    private Integer isFree;
}
