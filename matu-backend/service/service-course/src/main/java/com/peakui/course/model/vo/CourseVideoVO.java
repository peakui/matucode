package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVideoVO {
    private Long id;
    private Long chapterId;
    private String videoTitle;
    private String videoDesc;
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private String resolution;
    private Integer sortOrder;
    private Integer playCount;
    private Integer isFreePreview;
}
