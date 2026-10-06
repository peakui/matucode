package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_members")
public class ClassMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long classId;
    private Long userId;
    private Integer role;
    private Integer joinStatus;
    private LocalDateTime joinedAt;
}
