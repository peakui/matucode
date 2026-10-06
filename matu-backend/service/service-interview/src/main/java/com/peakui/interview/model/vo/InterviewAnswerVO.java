package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "面试答案信息")
public class InterviewAnswerVO {
    private Long id;
    private Long questionId;
    private Integer answerType;
    private Long userId;
    private String content;
    private Integer contentType;
    private String codeSnippet;
    private Integer likeCount;
    private Integer isOfficial;
    private Integer isLocked;
    private Integer unlockDays;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
