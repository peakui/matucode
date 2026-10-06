package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_assignments")
public class ClassAssignment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private String title;
    private String description;
    private Integer type;
    private String problemIds;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private Integer maxAttempts;
    private Integer isPublicRank;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
