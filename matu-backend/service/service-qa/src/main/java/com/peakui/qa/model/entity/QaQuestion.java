package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题。
 */
@Data
@TableName("qa_questions")
public class QaQuestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String content;
    private Integer bountyPoints;
    private Integer viewCount;
    private Integer answerCount;
    private Integer followCount;
    private Integer shareCount;
    private Integer status;
    private Long bestAnswerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime solvedAt;
}
