package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "模拟面试信息")
public class MockInterviewVO {
    private Long id;
    private Long userId;
    private String interviewTitle;
    private String position;
    private String company;
    private List<Long> questionIds;
    private Integer totalScore;
    private Integer userScore;
    private Integer durationMinutes;
    private Integer actualDuration;
    private Integer status;
    private String reportJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
