package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oj_submissions")
public class OjSubmission {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long problemId;
    private Long userId;
    private Long assignmentId;
    private String language;
    private String code;
    private Integer codeLength;
    private Integer status;
    private Integer executionTime;
    private Integer memoryUsed;
    private BigDecimal passRate;
    private Integer passedCases;
    private Integer totalCases;
    private String errorMessage;
    private LocalDateTime judgeTime;
    private LocalDateTime createdAt;
}
