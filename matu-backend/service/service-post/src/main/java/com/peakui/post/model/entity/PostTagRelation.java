package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子标签关联表。
 */
@Data
@TableName("post_tag_relations")
public class PostTagRelation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Long tagId;
    private LocalDateTime createdAt;
}
