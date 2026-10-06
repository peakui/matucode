package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回答。
 */
@Data
@TableName("qa_answers")
public class QaAnswer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer isAccepted;
    private LocalDateTime acceptedAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
