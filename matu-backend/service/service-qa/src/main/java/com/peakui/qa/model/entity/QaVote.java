package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票。
 */
@Data
@TableName("qa_votes")
public class QaVote {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Integer targetType;
    private Long targetId;
    private Long userId;
    private Integer voteType;
    private LocalDateTime createdAt;
}
