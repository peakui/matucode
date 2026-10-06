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
public class CourseReviewVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private Integer rating;
    private String content;
    private Integer likeCount;
    private Integer isVerifiedPurchase;
    private LocalDateTime createdAt;
}
