package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞表。
 */
@Data
@TableName("post_likes")
public class PostLike {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}
