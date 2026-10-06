package com.peakui.qa.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 词云图。
 */
@Data
@TableName("qa_word_clouds")
public class QaWordCloud {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long questionId;
    private String wordData;
    private String imageUrl;
    private LocalDateTime generatedAt;
    private Integer status;
}
