package com.peakui.qa.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新回答请求。
 */
@Data
public class UpdateAnswerRequest {

    @NotBlank(message = "回答内容不能为空")
    private String content;
}
