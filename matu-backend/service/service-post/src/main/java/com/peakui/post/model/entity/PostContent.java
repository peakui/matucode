package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子内容表。
 */
@Data
@TableName("post_contents")
public class PostContent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Integer contentType;
    private String content;
    private Integer wordCount;
    private Integer readTime;
    private Integer version;
    private Long lastEditUserId;
    private LocalDateTime lastEditTime;
}
