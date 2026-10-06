package com.peakui.info.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackListItemVO {

    private Long id;
    private Long userId;
    private String username;
    private String contactEmail;
    private Integer type;
    private String title;
    private Integer status;
    private Integer priority;
    private Long assigneeId;
    private LocalDateTime repliedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
