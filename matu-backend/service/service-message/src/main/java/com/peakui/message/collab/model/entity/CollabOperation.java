package com.peakui.message.collab.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("collab_operations")
public class CollabOperation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long documentId;
    private Long conversationId;
    private Long revision;
    private Long baseRevision;
    private Long userId;
    private String clientId;
    private String requestId;
    private Integer operationType;
    private Integer position;
    private String text;
    private Integer length;
    private Integer transformedPosition;
    private LocalDateTime createdAt;
}
