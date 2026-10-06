package com.peakui.oj.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "班级/竞赛 ACM 排行榜")
public class OjClassRankingVO {

    @Schema(description = "班级/竞赛ID", example = "1")
    private Long classId;

    @Schema(description = "名称", example = "2026春季算法赛")
    private String name;

    @Schema(description = "开始时间（罚时基准）", example = "2026-04-27T10:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-04-27T12:00:00")
    private LocalDateTime endTime;

    @Schema(description = "参与人数（已加入成员）", example = "35")
    private Integer memberCount;

    @Schema(description = "题目列表（榜单列头）")
    private List<ProblemBrief> problems;

    @Schema(description = "榜单行，已按 ACM 规则排序")
    private List<Row> rows;

    @Data
    @Builder
    @Schema(description = "榜单题目列")
    public static class ProblemBrief {

        @Schema(description = "题目ID", example = "1001")
        private Long problemId;

        @Schema(description = "题目编号", example = "A1001")
        private String problemNo;

        @Schema(description = "题目标题", example = "A+B Problem")
        private String title;

        @Schema(description = "榜单列序号（1 起，对应 A/B/C...）", example = "1")
        private Integer index;
    }

    @Data
    @Builder
    @Schema(description = "排行榜单行")
    public static class Row {

        @Schema(description = "名次", example = "1")
        private Integer rank;

        @Schema(description = "用户ID", example = "1001")
        private Long userId;

        @Schema(description = "昵称", example = "Alice")
        private String userName;

        @Schema(description = "头像", example = "https://xxx.com/a.png")
        private String avatarUrl;

        @Schema(description = "通过题数", example = "3")
        private Integer solvedCount;

        @Schema(description = "总罚时（分钟）", example = "120")
        private Long penaltyMinutes;

        @Schema(description = "最后一次通过时间", example = "2026-04-27T11:20:00")
        private LocalDateTime lastAcAt;

        @Schema(description = "各题作答明细")
        private List<Cell> cells;
    }

    @Data
    @Builder
    @Schema(description = "排行榜单元格（某用户某题）")
    public static class Cell {

        @Schema(description = "题目ID", example = "1001")
        private Long problemId;

        @Schema(description = "是否通过", example = "true")
        private Boolean solved;

        @Schema(description = "通过前的错误提交次数", example = "2")
        private Integer wrongAttempts;

        @Schema(description = "从开始时间到首次通过经过的分钟数", example = "35")
        private Long acMinutes;

        @Schema(description = "首次通过时间", example = "2026-04-27T10:35:00")
        private LocalDateTime acAt;
    }
}
