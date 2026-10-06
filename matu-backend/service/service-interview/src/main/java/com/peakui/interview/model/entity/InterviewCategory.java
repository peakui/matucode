package com.peakui.interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_categories")
public class InterviewCategory {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer questionCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
