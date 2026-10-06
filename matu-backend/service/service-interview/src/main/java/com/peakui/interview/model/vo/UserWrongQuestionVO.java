package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "错题本信息")
public class UserWrongQuestionVO {
    private Long id;
    private Long userId;
    private Long questionId;
    private String questionTitle;
    private Integer questionType;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
    private String wrongReason;
    private Integer isResolved;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
