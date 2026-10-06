package com.peakui.check.service;

import com.peakui.check.model.dto.ClaimAchievementRewardRequest;
import com.peakui.check.model.dto.CreateCheckCommentRequest;
import com.peakui.check.model.dto.CreateCheckGroupRequest;
import com.peakui.check.model.dto.CreateCheckRecordRequest;
import com.peakui.check.model.dto.OperateGroupMemberRequest;
import com.peakui.check.model.dto.UpdateCheckRecordRequest;
import com.peakui.check.model.vo.CheckCommentVO;
import com.peakui.check.model.vo.CheckDaysStatisticsVO;
import com.peakui.check.model.vo.CheckGroupVO;
import com.peakui.check.model.vo.CheckRecordListItemVO;
import com.peakui.check.model.vo.CheckRecordVO;
import com.peakui.check.model.vo.CheckStatisticsVO;
import com.peakui.check.model.vo.UserAchievementVO;
import com.peakui.common.result.PageResponse;

import java.util.List;

/**
 * 打卡业务接口。
 */
public interface CheckService {

    CheckRecordVO createRecord(CreateCheckRecordRequest request);

    CheckRecordVO updateRecord(Long checkId, UpdateCheckRecordRequest request);

    void deleteRecord(Long checkId);

    CheckRecordVO getRecordDetail(Long checkId);

    PageResponse<CheckRecordListItemVO> listRecords(Long userId, Integer status, Integer year, Integer month,
                                                    Long pageNum, Long pageSize);

    PageResponse<CheckRecordListItemVO> listLikedCheckRecords(Long pageNum, Long pageSize);

    CheckStatisticsVO getStatistics(Long userId, Integer year, Integer month);

    CheckDaysStatisticsVO getCheckDays(Long userId);

    List<UserAchievementVO> listAchievements(Long userId);

    void likeRecord(Long checkId);

    void unlikeRecord(Long checkId);

    void shareRecord(Long checkId);

    List<CheckCommentVO> listComments(Long checkId);

    CheckCommentVO createComment(Long checkId, CreateCheckCommentRequest request);

    void deleteComment(Long checkId, Long commentId);

    void likeComment(Long checkId, Long commentId);

    void unlikeComment(Long checkId, Long commentId);

    CheckGroupVO createGroup(CreateCheckGroupRequest request);

    List<CheckGroupVO> listPublicGroups();

    void joinGroup(Long groupId);

    void quitGroup(Long groupId);

    void removeGroupMember(Long groupId, OperateGroupMemberRequest request);

    void claimAchievementReward(ClaimAchievementRewardRequest request);
}
