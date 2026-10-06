package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailVO {
    private Long id;
    private Long instructorId;
    private String instructorName;
    private String instructorNickname;
    private String instructorAvatarUrl;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String description;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer level;
    private String language;
    private Integer studentCount;
    private Integer chapterCount;
    private Integer videoCount;
    private Integer totalDuration;
    private BigDecimal rating;
    private Integer ratingCount;
    private Integer isFree;
    private LocalDateTime publishedAt;
    private Boolean vipRequired;
    private Boolean canWatch;
    private List<CourseChapterVO> chapters;
    private List<CourseArticleVO> articles;
}
