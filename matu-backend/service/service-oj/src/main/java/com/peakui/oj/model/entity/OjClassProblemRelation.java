package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oj_class_problem_relations")
public class OjClassProblemRelation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private Long problemId;
    private Long baseProblemId;
    private Integer relationType;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
