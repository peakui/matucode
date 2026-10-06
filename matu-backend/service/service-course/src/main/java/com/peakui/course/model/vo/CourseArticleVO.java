package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseArticleVO {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String content;
    private Integer wordCount;
    private Integer readTime;
    private Integer viewCount;
    private Integer sortOrder;
}
