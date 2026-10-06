package com.peakui.message.collab.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCollabDocumentRequest {
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;
    private String content;
}
