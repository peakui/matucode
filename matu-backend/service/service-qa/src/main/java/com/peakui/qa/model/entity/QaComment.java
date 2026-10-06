package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答评论。
 */
@Data
@TableName("qa_comments")
public class QaComment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Integer targetType;
    private Long targetId;
    private Long userId;
    private Long parentId;
    private Long replyToUserId;
    private Integer replyCount;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
