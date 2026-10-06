package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞记录。
 */
@Data
@TableName("comment_likes")
public class CommentLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long commentId;
    private Long userId;
    private LocalDateTime createdAt;
}
