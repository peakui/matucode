package com.peakui.course.model.dto;

import lombok.Data;

@Data
public class UpdateVideoRequest {
    private Long chapterId;
    private String videoTitle;
    private String videoDesc;
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private Integer sortOrder;
    private Integer isFreePreview;
    private Integer status;
}
