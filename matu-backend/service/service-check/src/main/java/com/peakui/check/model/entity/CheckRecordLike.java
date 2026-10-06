package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡点赞记录。
 */
@Data
@TableName("check_record_likes")
public class CheckRecordLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long checkId;
    private Long userId;
    private LocalDateTime createdAt;
}
