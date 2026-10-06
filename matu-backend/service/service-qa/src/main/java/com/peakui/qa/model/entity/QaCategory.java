package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答分类。
 */
@Data
@TableName("qa_categories")
public class QaCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer sortOrder;
    private Integer questionCount;
    private Integer status;
    private LocalDateTime createdAt;
}
