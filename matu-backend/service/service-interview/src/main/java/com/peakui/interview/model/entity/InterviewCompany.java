package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_companies")
public class InterviewCompany {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String companyName;
    private String companyLogo;
    private String companyType;
    private Integer questionCount;
    private Integer status;
    private LocalDateTime createdAt;
}
