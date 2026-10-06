package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡评论表。
 */
@Data
@TableName("check_comments")
public class CheckComment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long checkId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private Integer replyCount;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
