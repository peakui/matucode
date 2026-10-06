package com.peakui.message.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Integer conversationType;
    private String conversationName;
    private Long creatorId;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer memberCount;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
