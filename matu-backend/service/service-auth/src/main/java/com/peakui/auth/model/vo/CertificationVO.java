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
public class CertificationVO {

    private Long id;
    private Long userId;
    private Integer certType;
    private String certTypeName;
    private String certName;
    private String certProof;
    private Integer certStatus;
    private String certStatusName;
    private String auditRemark;
    private Long auditorId;
    private LocalDateTime auditTime;
    private LocalDateTime createdAt;
}
