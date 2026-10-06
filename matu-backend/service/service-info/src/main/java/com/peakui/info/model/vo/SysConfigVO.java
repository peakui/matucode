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
public class SysConfigVO {

    private String configKey;
    private String configValue;
    private String description;
    private String groupName;
    private Integer isPublic;
    private LocalDateTime updatedAt;
}
