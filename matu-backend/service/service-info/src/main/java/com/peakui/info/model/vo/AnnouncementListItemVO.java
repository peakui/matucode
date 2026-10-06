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
public class AnnouncementListItemVO {

    private Long id;
    private String title;
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
