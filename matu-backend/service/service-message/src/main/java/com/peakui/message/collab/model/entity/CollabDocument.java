package com.peakui.message.collab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("collab_documents")
public class CollabDocument {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private String title;
    private String content;
    private Long revision;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
