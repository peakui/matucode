package com.peakui.course.model.dto;

import lombok.Data;

@Data
public class UpdateArticleRequest {
    private Long chapterId;
    private String title;
    private String content;
    private Integer sortOrder;
    private Integer status;
}
