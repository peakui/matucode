package com.peakui.oj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("oj_problem_contents")
public class OjProblemContent {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long problemId;
    private String contentMd;
    private String contentHtml;
    private String starterCodeJson;
    private String solutionJson;
    private String tagsJson;
    private Integer version;
}
