package com.peakui.info.model.vo;

import com.peakui.info.model.dto.FeedbackAttachmentItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDetailVO {
    private List<com.peakui.info.model.entity.FeedbackHistory> history;

    private Long id;
    private Long userId;
    private String username;
    private String contactEmail;
    private Integer type;
    private String title;
    private String content;
    private List<FeedbackAttachmentItem> attachments;
    private Map<String, Object> extraInfo;
    private Integer status;
    private Integer priority;
    private Long assigneeId;
    private String replyContent;
    private LocalDateTime repliedAt;
    private LocalDateTime resolvedAt;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
