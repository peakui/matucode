package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_question_progress")
public class UserQuestionProgress {
    @TableId(type = IdType.ASSIGN_ID)
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
