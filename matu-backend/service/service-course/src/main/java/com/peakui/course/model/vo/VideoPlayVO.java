package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayVO {
    private Long courseId;
    private Long chapterId;
    private Long videoId;
    private String title;
    private String videoUrl;
    private Boolean freePreview;
    private Boolean vipRequired;
    private Boolean playable;
    private String message;
}
