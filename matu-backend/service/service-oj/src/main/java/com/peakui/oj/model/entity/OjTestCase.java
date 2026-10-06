package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("oj_test_cases")
public class OjTestCase {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long problemId;
    private Integer caseNo;
    private String input;
    private String expectedOutput;
    private Integer isSample;
    private java.math.BigDecimal scoreWeight;
    private Integer isHidden;
    private java.time.LocalDateTime createdAt;
}
