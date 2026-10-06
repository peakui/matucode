package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签表。
 */
@Data
@TableName("post_tags")
public class PostTag {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tagName;
    private String tagDesc;
    private Integer postCount;
    private Integer status;
    private LocalDateTime createdAt;
}
