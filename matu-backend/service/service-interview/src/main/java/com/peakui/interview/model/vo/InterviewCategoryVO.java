package com.peakui.interview.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "面试分类信息")
public class InterviewCategoryVO {
    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryDesc;
    private String iconUrl;
    private Integer questionCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private List<InterviewCategoryVO> children;
}
