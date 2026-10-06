package com.peakui.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表实体。
 */
@Data
@TableName("roles")
public class Role {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("role_desc")
    private String roleDesc;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
