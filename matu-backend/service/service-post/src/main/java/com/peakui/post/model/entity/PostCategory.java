package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子分类表。
 */
@Data
@TableName("post_categories")
public class PostCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer sortOrder;
    private Integer postCount;
    private Integer status;
    private LocalDateTime createdAt;
}
