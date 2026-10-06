package com.peakui.interview.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateInterviewCategoryRequest {
    private Long parentId;

    @NotBlank
    private String categoryName;

    private String categoryDesc;
    private String iconUrl;
    private Integer sortOrder;
    private Integer status;
}
