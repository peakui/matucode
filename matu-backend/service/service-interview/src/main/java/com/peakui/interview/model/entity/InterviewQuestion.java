package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_questions")
public class InterviewQuestion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String questionNo;
    private String title;
    private String content;
    private String answer;
    private Long categoryId;
    private Long companyId;
    private Long publisherId;
    private String positionTags;
    private Integer difficulty;
    private Integer frequency;
    private Integer viewCount;
    private Integer collectCount;
    private Integer isLocked;
    private Integer unlockDays;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
