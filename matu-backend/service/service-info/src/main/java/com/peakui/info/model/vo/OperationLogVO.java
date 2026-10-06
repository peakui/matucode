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
public class OperationLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private Integer result;
    private String errorMsg;
    private LocalDateTime createdAt;
}
