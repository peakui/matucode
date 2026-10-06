package com.peakui.post.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子编辑历史表。
 */
@Data
@TableName("post_edit_history")
public class PostEditHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;
    private Integer version;
    private Long editorId;
    private String editorName;
    private String contentSnapshot;
    private String changeDesc;
    private Integer changeType;
    private LocalDateTime createdAt;
}
