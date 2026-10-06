package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oj_problems")
public class OjProblem {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String problemNo;
    private String title;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String sampleInput;
    private String sampleOutput;
    private String hint;
    private Integer difficulty;
    private Long categoryId;
    private Integer timeLimit;
    private Integer memoryLimit;
    private Integer submitCount;
    private Integer acceptCount;
    private BigDecimal acceptRate;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
