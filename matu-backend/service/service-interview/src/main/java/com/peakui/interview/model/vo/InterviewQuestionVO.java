package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "面试题信息")
public class InterviewQuestionVO {
    private Long id;
    private String questionNo;
    private String title;
    private String content;
    private String answer;
    private Long categoryId;
    private String categoryName;
    private Long companyId;
    private String companyName;
    private Long publisherId;
    private List<String> positionTags;
    private Integer difficulty;
    private Integer frequency;
    private Integer viewCount;
    private Integer collectCount;
    private Integer isLocked;
    private Boolean answerVisible;
    private Integer unlockDays;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
