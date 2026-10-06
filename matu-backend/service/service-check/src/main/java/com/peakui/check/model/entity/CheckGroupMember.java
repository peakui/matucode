package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡小组成员表。
 */
@Data
@TableName("check_group_members")
public class CheckGroupMember {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupId;
    private Long userId;
    private Integer role;
    private LocalDateTime joinedAt;
    private Integer status;
}
