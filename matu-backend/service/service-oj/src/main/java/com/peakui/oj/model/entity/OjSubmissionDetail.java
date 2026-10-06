package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oj_submission_details")
public class OjSubmissionDetail {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long submissionId;
    private Integer caseNo;
    @TableField(exist = false)
    private String input;
    @TableField(exist = false)
    private String expectedOutput;
    @TableField(exist = false)
    private String actualOutput;
    private Integer status;
    private Integer executionTime;
    private Integer memoryUsed;
    @TableField(exist = false)
    private String errorMessage;
    private LocalDateTime createdAt;
}
