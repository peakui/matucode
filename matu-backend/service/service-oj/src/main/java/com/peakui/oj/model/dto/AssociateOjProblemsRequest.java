package com.peakui.oj.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "关联已有OJ题目请求")
public class AssociateOjProblemsRequest {
    @Schema(description = "题目ID列表", example = "[1,2,3]")
    @NotEmpty(message = "题目ID不能为空")
    private List<Long> problemIds;
}
