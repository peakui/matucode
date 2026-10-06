package com.peakui.auth.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VipStatusVO {
    private Long userId;
    private Boolean valid;
    private Integer isVip;
    private Integer vipLevel;
    private LocalDateTime vipExpiredAt;
    private Integer vipDaysRemaining;
}
