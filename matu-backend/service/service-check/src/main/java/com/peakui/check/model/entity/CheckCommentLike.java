package com.peakui.check.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 打卡评论点赞记录。
 */
@Data
@TableName("check_comment_likes")
public class CheckCommentLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long commentId;
    private Long userId;
    private LocalDateTime createdAt;
}
