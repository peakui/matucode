package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("certifications")
public class Certification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("cert_type")
    private Integer certType;

    @TableField("cert_name")
    private String certName;

    @TableField("cert_proof")
    private String certProof;

    @TableField("cert_status")
    private Integer certStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("auditor_id")
    private Long auditorId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
