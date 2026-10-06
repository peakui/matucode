package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子协作者表。
 */
@Data
@TableName("post_collaborators")
public class PostCollaborator {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Long userId;
    private Integer permission;
    private Long invitedBy;
    private LocalDateTime joinedAt;
}
