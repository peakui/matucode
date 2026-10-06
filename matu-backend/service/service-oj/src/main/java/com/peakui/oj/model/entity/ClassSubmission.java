package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("class_submissions")
public class ClassSubmission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long assignmentId;
    private Long userId;
    private Long problemId;
    private Long submissionId;
    private Integer status;
    private BigDecimal score;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
