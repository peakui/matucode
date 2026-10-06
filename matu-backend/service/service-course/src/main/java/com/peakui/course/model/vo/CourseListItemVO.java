package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListItemVO {
    private Long id;
    private Long instructorId;
    private String instructorName;
    private String instructorNickname;
    private String instructorAvatarUrl;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String coverUrl;
    private BigDecimal price;
    private Integer level;
    private Integer studentCount;
    private Integer chapterCount;
    private Integer videoCount;
    private Integer totalDuration;
    private BigDecimal rating;
    private Integer isFree;
    private LocalDateTime publishedAt;
}
