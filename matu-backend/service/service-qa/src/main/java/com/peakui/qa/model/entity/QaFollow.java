package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题关注。
 */
@Data
@TableName("qa_follows")
public class QaFollow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionId;
    private Long userId;
    private LocalDateTime createdAt;
}
