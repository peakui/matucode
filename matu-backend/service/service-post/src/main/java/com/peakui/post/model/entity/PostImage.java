package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子图片表。
 */
@Data
@TableName("post_images")
public class PostImage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private String imageUrl;
    private String imageType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
