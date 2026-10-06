package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子主表。
 */
@Data
@TableName("posts")
public class Post {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long categoryId;
    private String title;
    private String summary;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer shareCount;
    private Integer status;
    private Integer isTop;
    private Integer isEssence;
    private Integer isLock;
    private String lockReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
