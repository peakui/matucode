package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "面试公司信息")
public class InterviewCompanyVO {
    private Long id;
    private String companyName;
    private String companyLogo;
    private String companyType;
    private Integer questionCount;
    private Integer status;
    private LocalDateTime createdAt;
}
