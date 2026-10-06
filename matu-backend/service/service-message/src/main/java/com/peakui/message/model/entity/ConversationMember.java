package com.peakui.message.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_members")
public class ConversationMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long userId;
    private Integer role;
    private Integer isMuted;
    private Integer isTop;
    private Long lastReadMessageId;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
