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
public class CourseProgressVO {
    private Long id;
    private Long courseId;
    private Long videoId;
    private BigDecimal progressPercent;
    private Integer watchedDuration;
    private LocalDateTime lastWatchTime;
    private Integer isCompleted;
    private LocalDateTime completedAt;
}
