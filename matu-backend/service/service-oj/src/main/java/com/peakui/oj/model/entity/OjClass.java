package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("classes")
public class OjClass {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer type;
    private String description;
    private Long creatorId;
    private String coverImage;
    private Integer joinMode;
    private String inviteCode;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
}
