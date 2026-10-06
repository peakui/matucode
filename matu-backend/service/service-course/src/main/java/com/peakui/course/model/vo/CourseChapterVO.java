package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseChapterVO {
    private Long id;
    private Long courseId;
    private String chapterTitle;
    private String chapterDesc;
    private Integer sortOrder;
    private Integer videoCount;
    private Integer duration;
    private Integer isFreePreview;
    private List<CourseVideoVO> videos;
}
