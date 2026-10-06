package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "作业排行榜")
public class AssignmentRankingVO {

    @Schema(description = "作业ID", example = "1")
    private Long assignmentId;

    @Schema(description = "作业题目总数", example = "5")
    private Integer totalProblems;

    @Schema(description = "是否公开榜单 0-否 1-是", example = "1")
    private Integer isPublicRank;

    @Schema(description = "榜单行")
    private List<RankRow> rows;

    @Data
    @Builder
    @Schema(description = "排行榜单行")
    public static class RankRow {

        @Schema(description = "名次", example = "1")
        private Integer rank;

        @Schema(description = "用户ID", example = "1001")
        private Long userId;

        @Schema(description = "昵称", example = "Alice")
        private String nickname;

        @Schema(description = "头像", example = "https://xxx.com/a.png")
        private String avatarUrl;

        @Schema(description = "已通过题目数", example = "3")
        private Integer solvedCount;

        @Schema(description = "得分", example = "300.00")
        private BigDecimal score;

        @Schema(description = "最后一次通过时间", example = "2026-04-27T10:00:00")
        private LocalDateTime lastSubmitAt;
    }
}
