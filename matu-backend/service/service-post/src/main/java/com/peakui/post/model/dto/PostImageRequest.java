package com.peakui.post.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 帖子图片请求。
 */
@Data
public class PostImageRequest {

    @NotBlank(message = "图片地址不能为空")
    @Size(max = 500, message = "图片地址长度不能超过500个字符")
    private String imageUrl;

    @Size(max = 20, message = "图片类型长度不能超过20个字符")
    private String imageType;

    private Integer sortOrder;
}
