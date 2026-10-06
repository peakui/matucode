package com.peakui.check.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打卡小组返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "打卡小组")
public class CheckGroupVO {

    private Long id;
    private String groupName;
    private String groupDesc;
    private String coverImage;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String creatorSchoolName;
    private String creatorCompanyName;
    private String creatorTitle;
    private Integer memberCount;
    private Integer articleCount;
    private Integer isPublic;
    private Integer status;
}
