package com.peakui.course.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCertificateVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String certificateNo;
    private String certificateUrl;
    private LocalDate issueDate;
    private LocalDate expireDate;
}
