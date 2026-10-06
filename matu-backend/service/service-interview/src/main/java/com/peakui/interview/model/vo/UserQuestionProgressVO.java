package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "用户题目进度信息")
public class UserQuestionProgressVO {
    private Long id;
    private Long userId;
    private Long questionId;
    private Integer questionType;
    private Integer status;
    private String answerContent;
    private LocalDateTime lastPracticeTime;
    private Integer practiceCount;
    private Integer masteryLevel;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
