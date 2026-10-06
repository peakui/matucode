package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseNoteVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long videoId;
    private String content;
    private Integer timestamp;
    private Integer isPublic;
    private Integer likeCount;
    private LocalDateTime createdAt;
}
