package com.peakui.message.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMessageVO {

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
}
