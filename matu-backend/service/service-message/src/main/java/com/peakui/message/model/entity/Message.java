package com.peakui.message.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Integer messageType;
    private String content;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Long replyToId;
    private Integer isDeleted;
    private Integer isRecall;
    private LocalDateTime recallTime;
    private Integer readStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
