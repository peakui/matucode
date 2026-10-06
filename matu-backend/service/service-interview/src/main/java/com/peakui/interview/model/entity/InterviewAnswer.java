package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_answers")
public class InterviewAnswer {
    @TableId(type = IdType.ASSIGN_ID)
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
