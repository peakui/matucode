package com.peakui.post.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建帖子请求。
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    private Long categoryId;

    @NotNull(message = "内容类型不能为空")
    private Integer contentType;

    @NotBlank(message = "正文不能为空")
    private String content;

    private Integer status;

    @Valid
    private List<PostImageRequest> images;

    @Size(max = 10, message = "标签数量不能超过10个")
    private List<@NotBlank(message = "标签名称不能为空") @Size(max = 50, message = "标签名称长度不能超过50个字符") String> tagNames;
}
