package com.peakui.message.collab.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TextOperationDTO {
    @NotBlank(message = "操作类型不能为空")
    private String op;
    @Min(value = 0, message = "操作位置不能小于0")
    private Integer position;
    private String text;
    private Integer length;
}
