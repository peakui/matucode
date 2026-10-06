package com.peakui.check.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.check.feign.UserFeignClient;
import com.peakui.check.feign.vo.UserProfileVO;
import com.peakui.check.mapper.CheckRecordMapper;
import com.peakui.check.mapper.CheckStatisticMapper;
import com.peakui.check.model.dto.ClaimAchievementRewardRequest;
import com.peakui.check.model.dto.CreateCheckCommentRequest;
import com.peakui.check.model.dto.CreateCheckGroupRequest;
import com.peakui.check.model.dto.CreateCheckRecordRequest;
import com.peakui.check.model.dto.OperateGroupMemberRequest;
import com.peakui.check.model.dto.UpdateCheckRecordRequest;
import com.peakui.check.model.entity.CheckRecord;
import com.peakui.check.model.entity.CheckStatistic;
import com.peakui.check.model.vo.CheckCommentVO;
import com.peakui.check.model.vo.CheckDaysStatisticsVO;
import com.peakui.common.exception.CommonError;
import com.peakui.check.model.vo.CheckGroupVO;
import com.peakui.check.model.vo.CheckRecordListItemVO;
import com.peakui.check.model.vo.CheckRecordVO;
import com.peakui.check.model.vo.CheckStatisticsVO;
import com.peakui.check.model.vo.UserAchievementVO;
import com.peakui.check.service.CheckService;
import com.peakui.common.dashboard.DashboardCheckStatsVO;
import com.peakui.common.dashboard.DashboardCheckinRankItemDTO;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 打卡控制器。
 */
@Tag(name = "打卡接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/checks")
public class CheckController {

    private final CheckService checkService;
    private final CheckRecordMapper checkRecordMapper;
    private final CheckStatisticMapper checkStatisticMapper;
    private final UserFeignClient userFeignClient;

    @Operation(summary = "打卡数据总览统计")
    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardCheckStatsVO> dashboardStats() {
        Long checkinCount = checkRecordMapper.selectCount(new LambdaQueryWrapper<CheckRecord>().eq(CheckRecord::getStatus, 1));
        List<DashboardCheckinRankItemDTO> ranking = checkStatisticMapper.selectList(new LambdaQueryWrapper<CheckStatistic>()
                        .orderByDesc(CheckStatistic::getTotalDays)
                        .last("LIMIT 10"))
                .stream()
                .map(this::toCheckinRankItem)
                .toList();
        return ApiResponse.success(DashboardCheckStatsVO.builder()
                .checkinCount(checkinCount)
                .checkinRanking(ranking)
                .build());
    }

    private DashboardCheckinRankItemDTO toCheckinRankItem(CheckStatistic statistic) {
        ApiResponse<UserProfileVO> response = userFeignClient.getUserProfile(statistic.getUserId());
        UserProfileVO user = response == null ? null : response.getData();
        return DashboardCheckinRankItemDTO.builder()
                .userId(String.valueOf(statistic.getUserId()))
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .avatar(user == null ? null : user.getAvatarUrl())
                .checkinCount(statistic.getTotalDays() == null ? 0L : statistic.getTotalDays().longValue())
                .continuousDays(statistic.getContinuousDays() == null ? 0 : statistic.getContinuousDays())
                .totalLearnHours(statistic.getTotalLearnHours() == null ? BigDecimal.ZERO : statistic.getTotalLearnHours())
                .build();
    }

    @Operation(summary = "创建打卡")
    @PostMapping
    public ApiResponse<CheckRecordVO> createRecord(@Valid @RequestBody CreateCheckRecordRequest request) {
        return ApiResponse.success("打卡成功", checkService.createRecord(request));
    }

    @Operation(summary = "更新打卡")
    @PutMapping("/{checkId}")
    public ApiResponse<CheckRecordVO> updateRecord(@PathVariable Long checkId,
                                                   @Valid @RequestBody UpdateCheckRecordRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), checkService.updateRecord(checkId, request));
    }

    @Operation(summary = "删除打卡")
    @DeleteMapping("/{checkId}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long checkId) {
        checkService.deleteRecord(checkId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "打卡详情")
    @GetMapping("/{checkId}")
    public ApiResponse<CheckRecordVO> getRecordDetail(@PathVariable Long checkId) {
        return ApiResponse.success(checkService.getRecordDetail(checkId));
    }

    @Operation(summary = "打卡列表")
    @GetMapping
    public ApiResponse<PageResponse<CheckRecordListItemVO>> listRecords(@RequestParam(required = false) Long userId,
                                                                        @RequestParam(required = false) Integer status,
                                                                        @RequestParam(required = false) Integer year,
                                                                        @RequestParam(required = false) Integer month,
                                                                        @RequestParam(defaultValue = "1") Long pageNum,
                                                                        @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(checkService.listRecords(userId, status, year, month, pageNum, pageSize));
    }

    @Operation(summary = "我点赞的打卡")
    @GetMapping("/liked")
    public ApiResponse<PageResponse<CheckRecordListItemVO>> listLikedRecords(@RequestParam(defaultValue = "1") Long pageNum,
                                                                             @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(checkService.listLikedCheckRecords(pageNum, pageSize));
    }

    @Operation(summary = "点赞打卡")
    @PostMapping("/{checkId}/like")
    public ApiResponse<Void> likeRecord(@PathVariable Long checkId) {
        checkService.likeRecord(checkId);
        return ApiResponse.success(CommonError.LIKE_SUCCESS.message(), null);
    }

    @Operation(summary = "取消点赞打卡")
    @DeleteMapping("/{checkId}/like")
    public ApiResponse<Void> unlikeRecord(@PathVariable Long checkId) {
        checkService.unlikeRecord(checkId);
        return ApiResponse.success(CommonError.UNLIKE_SUCCESS.message(), null);
    }

    @Operation(summary = "分享打卡")
    @PostMapping("/{checkId}/share")
    public ApiResponse<Void> shareRecord(@PathVariable Long checkId) {
        checkService.shareRecord(checkId);
        return ApiResponse.success("分享成功", null);
    }

    @Operation(summary = "月度统计")
    @GetMapping("/statistics")
    public ApiResponse<CheckStatisticsVO> getStatistics(@RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) Integer year,
                                                        @RequestParam(required = false) Integer month) {
        return ApiResponse.success(checkService.getStatistics(userId, year, month));
    }

    @Operation(summary = "获取打卡天数")
    @GetMapping("/statistics/days")
    public ApiResponse<CheckDaysStatisticsVO> getCheckDays(@RequestParam(required = false) Long userId) {
        return ApiResponse.success(checkService.getCheckDays(userId));
    }

    @Operation(summary = "成就列表")
    @GetMapping("/achievements")
    public ApiResponse<List<UserAchievementVO>> listAchievements(@RequestParam(required = false) Long userId) {
        return ApiResponse.success(checkService.listAchievements(userId));
    }

    @Operation(summary = "领取成就奖励")
    @PostMapping("/achievements/claim")
    public ApiResponse<Void> claimAchievementReward(@Valid @RequestBody ClaimAchievementRewardRequest request) {
        checkService.claimAchievementReward(request);
        return ApiResponse.success("领取成功", null);
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{checkId}/comments")
    public ApiResponse<CheckCommentVO> createComment(@PathVariable Long checkId,
                                                     @Valid @RequestBody CreateCheckCommentRequest request) {
        return ApiResponse.success(CommonError.COMMENT_SUCCESS.message(), checkService.createComment(checkId, request));
    }

    @Operation(summary = "评论列表")
    @GetMapping("/{checkId}/comments")
    public ApiResponse<List<CheckCommentVO>> listComments(@PathVariable Long checkId) {
        return ApiResponse.success(checkService.listComments(checkId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{checkId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long checkId, @PathVariable Long commentId) {
        checkService.deleteComment(checkId, commentId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{checkId}/comments/{commentId}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long checkId, @PathVariable Long commentId) {
        checkService.likeComment(checkId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{checkId}/comments/{commentId}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long checkId, @PathVariable Long commentId) {
        checkService.unlikeComment(checkId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "创建打卡小组")
    @PostMapping("/groups")
    public ApiResponse<CheckGroupVO> createGroup(@Valid @RequestBody CreateCheckGroupRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), checkService.createGroup(request));
    }

    @Operation(summary = "公开小组列表")
    @GetMapping("/groups")
    public ApiResponse<List<CheckGroupVO>> listPublicGroups() {
        return ApiResponse.success(checkService.listPublicGroups());
    }

    @Operation(summary = "加入打卡小组")
    @PostMapping("/groups/{groupId}/join")
    public ApiResponse<Void> joinGroup(@PathVariable Long groupId) {
        checkService.joinGroup(groupId);
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), null);
    }

    @Operation(summary = "退出打卡小组")
    @PostMapping("/groups/{groupId}/quit")
    public ApiResponse<Void> quitGroup(@PathVariable Long groupId) {
        checkService.quitGroup(groupId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "移除小组成员")
    @PostMapping("/groups/{groupId}/remove-member")
    public ApiResponse<Void> removeGroupMember(@PathVariable Long groupId,
                                               @Valid @RequestBody OperateGroupMemberRequest request) {
        checkService.removeGroupMember(groupId, request);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }
}
