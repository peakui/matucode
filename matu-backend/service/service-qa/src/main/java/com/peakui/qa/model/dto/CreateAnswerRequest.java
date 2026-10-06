package com.peakui.qa.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建回答请求。
 */
@Data
public class CreateAnswerRequest {

    @NotBlank(message = "回答内容不能为空")
    private String content;
}
