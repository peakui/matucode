package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡小组表。
 */
@Data
@TableName("check_groups")
public class CheckGroup {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String groupName;
    private String groupDesc;
    private String coverImage;
    private Long creatorId;
    private Integer memberCount;
    private Integer articleCount;
    private Integer isPublic;
    private Integer status;
    private LocalDateTime createdAt;
}
