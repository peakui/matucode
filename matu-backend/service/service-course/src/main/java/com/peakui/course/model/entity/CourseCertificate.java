package com.peakui.course.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("course_certificates")
public class CourseCertificate {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long courseId;
    private String certificateNo;
    private String certificateUrl;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private Integer status;
    private LocalDateTime createdAt;
}
