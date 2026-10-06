package com.peakui.info.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcements")
public class Announcement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private String content;
    private Integer type;
    private Integer priority;
    private Integer isPinned;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Integer status;
    private Long authorId;
    private Integer clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
