package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_discussions")
public class ClassDiscussion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private Long assignmentId;
    private Long userId;
    private String title;
    private String content;
    private Integer isAnonymous;
    private LocalDateTime createdAt;
}
