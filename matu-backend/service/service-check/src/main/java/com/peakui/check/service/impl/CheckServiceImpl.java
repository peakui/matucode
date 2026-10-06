package com.peakui.check.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.check.exception.CheckException;
import com.peakui.check.feign.MessageFeignClient;
import com.peakui.check.feign.UserFeignClient;
import com.peakui.check.feign.vo.UserProfileVO;
import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.check.mapper.AchievementMapper;
import com.peakui.check.mapper.CheckCommentLikeMapper;
import com.peakui.check.mapper.CheckCommentMapper;
import com.peakui.check.mapper.CheckGroupMapper;
import com.peakui.check.mapper.CheckGroupMemberMapper;
import com.peakui.check.mapper.CheckRecordLikeMapper;
import com.peakui.check.mapper.CheckRecordMapper;
import com.peakui.check.mapper.CheckStatisticMapper;
import com.peakui.check.mapper.UserAchievementMapper;
import com.peakui.check.model.dto.ClaimAchievementRewardRequest;
import com.peakui.check.model.dto.CreateCheckCommentRequest;
import com.peakui.check.model.dto.CreateCheckGroupRequest;
import com.peakui.check.model.dto.CreateCheckRecordRequest;
import com.peakui.check.model.dto.OperateGroupMemberRequest;
import com.peakui.check.model.dto.UpdateCheckRecordRequest;
import com.peakui.check.model.entity.Achievement;
import com.peakui.check.model.entity.CheckComment;
import com.peakui.check.model.entity.CheckCommentLike;
import com.peakui.check.model.entity.CheckGroup;
import com.peakui.check.model.entity.CheckGroupMember;
import com.peakui.check.model.entity.CheckRecord;
import com.peakui.check.model.entity.CheckRecordLike;
import com.peakui.check.model.entity.CheckStatistic;
import com.peakui.check.model.entity.UserAchievement;
import com.peakui.check.model.vo.CheckCommentVO;
import com.peakui.check.model.vo.CheckDaysStatisticsVO;
import com.peakui.check.model.vo.CheckGroupVO;
import com.peakui.check.model.vo.CheckRecordListItemVO;
import com.peakui.check.model.vo.CheckRecordVO;
import com.peakui.check.model.vo.CheckStatisticsVO;
import com.peakui.check.model.vo.UserAchievementVO;
import com.peakui.check.service.CheckService;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打卡业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckServiceImpl implements CheckService {

    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_DRAFT = 2;
    private static final int STATUS_DELETED = 3;
    private static final int STATUS_PRIVATE = 4;
    private static final int GROUP_STATUS_NORMAL = 1;
    private static final int MEMBER_STATUS_NORMAL = 1;
    private static final int MEMBER_ROLE_OWNER = 2;
    private static final int COMMENT_STATUS_NORMAL = 1;
    private static final int ACHIEVEMENT_TYPE_CHECK = 1;
    private static final int CONDITION_TYPE_DAYS = 1;
    private static final int CONDITION_TYPE_COUNT = 2;
    private static final int CONDITION_TYPE_CONTINUOUS = 3;

    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordLikeMapper checkRecordLikeMapper;
    private final CheckStatisticMapper checkStatisticMapper;
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final CheckGroupMapper checkGroupMapper;
    private final CheckGroupMemberMapper checkGroupMemberMapper;
    private final CheckCommentMapper checkCommentMapper;
    private final CheckCommentLikeMapper checkCommentLikeMapper;
    private final UserFeignClient userFeignClient;
    private final MessageFeignClient messageFeignClient;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest servletRequest;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckRecordVO createRecord(CreateCheckRecordRequest request) {
        Long currentUserId = getCurrentUserId();
        validateCheckDate(request.getCheckDate());
        ensureUniqueCheckDate(currentUserId, request.getCheckDate(), null);

        LocalDateTime now = LocalDateTime.now();
        CheckRecord record = new CheckRecord();
        record.setUserId(currentUserId);
        record.setTitle(request.getTitle().trim());
        record.setSummary(normalizeText(request.getSummary(), 255));
        record.setContent(request.getContent().trim());
        record.setImageUrls(toJsonArray(request.getImageUrls()));
        record.setLearnHours(normalizeLearnHours(request.getLearnHours()));
        record.setMood(normalizeMood(request.getMood()));
        record.setLocation(normalizeText(request.getLocation(), 100));
        record.setIpAddress(limitLength(getClientIp(), 45));
        record.setViewCount(0);
        record.setLikeCount(0);
        record.setCommentCount(0);
        record.setIsTop(0);
        record.setIsFeatured(0);
        record.setStatus(normalizeRecordStatus(request.getStatus()));
        record.setCheckDate(request.getCheckDate());
        record.setCheckTime(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        checkRecordMapper.insert(record);

        refreshMonthlyStatistic(currentUserId, request.getCheckDate().getYear(), request.getCheckDate().getMonthValue());
        refreshAchievements(currentUserId);
        increaseActivityLevel(currentUserId);
        log.info("创建打卡记录成功, checkId={}, userId={}, checkDate={}", record.getId(), currentUserId, request.getCheckDate());
        return getRecordDetail(record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckRecordVO updateRecord(Long checkId, UpdateCheckRecordRequest request) {
        Long currentUserId = getCurrentUserId();
        CheckRecord record = getOwnedRecord(checkId, currentUserId);
        Long recordUserId = record.getUserId();
        int oldYear = record.getCheckDate().getYear();
        int oldMonth = record.getCheckDate().getMonthValue();

        if (request.getCheckDate() != null) {
            validateCheckDate(request.getCheckDate());
            ensureUniqueCheckDate(recordUserId, request.getCheckDate(), checkId);
            record.setCheckDate(request.getCheckDate());
        }
        if (StringUtils.hasText(request.getTitle())) {
            record.setTitle(request.getTitle().trim());
        }
        if (request.getSummary() != null) {
            record.setSummary(normalizeText(request.getSummary(), 255));
        }
        if (StringUtils.hasText(request.getContent())) {
            record.setContent(request.getContent().trim());
        }
        if (request.getImageUrls() != null) {
            record.setImageUrls(toJsonArray(request.getImageUrls()));
        }
        if (request.getLearnHours() != null) {
            record.setLearnHours(normalizeLearnHours(request.getLearnHours()));
        }
        if (request.getMood() != null) {
            record.setMood(normalizeMood(request.getMood()));
        }
        if (request.getLocation() != null) {
            record.setLocation(normalizeText(request.getLocation(), 100));
        }
        if (request.getStatus() != null) {
            record.setStatus(normalizeRecordStatus(request.getStatus()));
        }
        record.setUpdatedAt(LocalDateTime.now());
        checkRecordMapper.updateById(record);

        refreshMonthlyStatistic(recordUserId, oldYear, oldMonth);
        refreshMonthlyStatistic(recordUserId, record.getCheckDate().getYear(), record.getCheckDate().getMonthValue());
        refreshAchievements(recordUserId);
        log.info("更新打卡记录成功, checkId={}, userId={}", checkId, recordUserId);
        return getRecordDetail(checkId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long checkId) {
        Long currentUserId = getCurrentUserId();
        CheckRecord record = getOwnedRecord(checkId, currentUserId);
        Long recordUserId = record.getUserId();
        if (Objects.equals(record.getStatus(), STATUS_DELETED)) {
            return;
        }
        record.setStatus(STATUS_DELETED);
        record.setUpdatedAt(LocalDateTime.now());
        checkRecordMapper.updateById(record);
        refreshMonthlyStatistic(recordUserId, record.getCheckDate().getYear(), record.getCheckDate().getMonthValue());
        refreshAchievements(recordUserId);
        log.info("删除打卡记录成功, checkId={}, userId={}", checkId, recordUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckRecordVO getRecordDetail(Long checkId) {
        Long currentUserId = getCurrentUserIdNullable();
        CheckRecord record = getVisibleRecord(checkId, currentUserId);
        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
        record.setViewCount(safeInt(record.getViewCount()) + 1);
        return toRecordDetailVO(record, currentUserId);
    }

    @Override
    public PageResponse<CheckRecordListItemVO> listRecords(Long userId, Integer status, Integer year, Integer month,
                                                           Long pageNum, Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Long currentUserId = getCurrentUserIdNullable();

        LambdaQueryWrapper<CheckRecord> wrapper = new LambdaQueryWrapper<CheckRecord>()
                .ne(CheckRecord::getStatus, STATUS_DELETED);
        if (userId != null) {
            wrapper.eq(CheckRecord::getUserId, userId);
        }
        if (year != null) {
            wrapper.apply("YEAR(check_date) = {0}", year);
        }
        if (month != null) {
            wrapper.apply("MONTH(check_date) = {0}", month);
        }
        if (status != null) {
            wrapper.eq(CheckRecord::getStatus, status);
        } else if (userId == null || !Objects.equals(userId, currentUserId)) {
            wrapper.in(CheckRecord::getStatus, List.of(STATUS_PUBLISHED));
        }
        wrapper.orderByDesc(CheckRecord::getIsTop, CheckRecord::getCheckDate, CheckRecord::getCheckTime);

        Page<CheckRecord> page = checkRecordMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        List<CheckRecord> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), toRecordListItemVOs(records));
    }

    @Override
    public PageResponse<CheckRecordListItemVO> listLikedCheckRecords(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        Page<CheckRecordLike> likePage = checkRecordLikeMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<CheckRecordLike>()
                        .eq(CheckRecordLike::getUserId, currentUserId)
                        .orderByDesc(CheckRecordLike::getCreatedAt));
        List<CheckRecordLike> likes = likePage.getRecords();
        if (likes.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, likePage.getTotal(), Collections.emptyList());
        }

        List<Long> checkIds = likes.stream().map(CheckRecordLike::getCheckId).distinct().toList();
        Map<Long, CheckRecord> recordMap = checkRecordMapper.selectBatchIds(checkIds).stream()
                .filter(record -> !Objects.equals(record.getStatus(), STATUS_DELETED))
                .collect(Collectors.toMap(CheckRecord::getId, record -> record, (a, b) -> a));
        List<CheckRecord> ordered = likes.stream()
                .map(like -> recordMap.get(like.getCheckId()))
                .filter(Objects::nonNull)
                .toList();
        return PageResponse.of(currentPage, currentSize, likePage.getTotal(), toRecordListItemVOs(ordered));
    }

    private List<CheckRecordListItemVO> toRecordListItemVOs(List<CheckRecord> records) {
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(records.stream().map(CheckRecord::getUserId).collect(Collectors.toSet()));
        return records.stream()
                .map(record -> {
                    UserProfileVO userProfile = userProfiles.get(record.getUserId());
                    return CheckRecordListItemVO.builder()
                            .id(record.getId())
                            .userId(record.getUserId())
                            .username(resolveUserName(userProfile, record.getUserId()))
                            .avatarUrl(resolveUserAvatar(userProfile))
                            .schoolName(resolveSchoolName(userProfile))
                            .companyName(resolveCompanyName(userProfile))
                            .authorTitle(resolveTitle(userProfile))
                            .authorIsVip(resolveAuthorIsVip(userProfile))
                            .title(record.getTitle())
                            .summary(record.getSummary())
                            .learnHours(normalizeLearnHours(record.getLearnHours()))
                            .mood(record.getMood())
                            .location(record.getLocation())
                            .likeCount(safeInt(record.getLikeCount()))
                            .commentCount(safeInt(record.getCommentCount()))
                            .viewCount(safeInt(record.getViewCount()))
                            .isTop(safeInt(record.getIsTop()))
                            .isFeatured(safeInt(record.getIsFeatured()))
                            .status(record.getStatus())
                            .checkDate(record.getCheckDate())
                            .checkTime(record.getCheckTime())
                            .createdAt(record.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    public CheckStatisticsVO getStatistics(Long userId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year == null ? now.getYear() : year;
        int targetMonth = month == null ? now.getMonthValue() : month;
        Long targetUserId = resolveTargetUserId(userId);
        if (targetUserId == null) {
            return emptyStatistics(null, targetYear, targetMonth);
        }
        CheckStatistic statistic = checkStatisticMapper.selectOne(new LambdaQueryWrapper<CheckStatistic>()
                .eq(CheckStatistic::getUserId, targetUserId)
                .eq(CheckStatistic::getCheckYear, targetYear)
                .eq(CheckStatistic::getCheckMonth, targetMonth)
                .last("limit 1"));
        if (statistic == null) {
            refreshMonthlyStatistic(targetUserId, targetYear, targetMonth);
            statistic = checkStatisticMapper.selectOne(new LambdaQueryWrapper<CheckStatistic>()
                    .eq(CheckStatistic::getUserId, targetUserId)
                    .eq(CheckStatistic::getCheckYear, targetYear)
                    .eq(CheckStatistic::getCheckMonth, targetMonth)
                    .last("limit 1"));
        }
        if (statistic == null) {
            return emptyStatistics(targetUserId, targetYear, targetMonth);
        }
        return toStatisticsVO(statistic);
    }

    private CheckStatisticsVO emptyStatistics(Long userId, int year, int month) {
        return CheckStatisticsVO.builder()
                .userId(userId)
                .year(year)
                .month(month)
                .totalDays(0)
                .continuousDays(0)
                .maxContinuousDays(0)
                .totalArticles(0)
                .totalLikesReceived(0)
                .totalLearnHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    @Override
    public CheckDaysStatisticsVO getCheckDays(Long userId) {
        Long targetUserId = resolveTargetUserId(userId);
        if (targetUserId == null) {
            return CheckDaysStatisticsVO.builder().totalDays(0).build();
        }
        CheckSummary summary = buildCheckSummary(targetUserId);
        return CheckDaysStatisticsVO.builder()
                .userId(targetUserId)
                .totalDays(summary.getTotalDays())
                .build();
    }

    @Override
    public List<UserAchievementVO> listAchievements(Long userId) {
        Long targetUserId = resolveTargetUserId(userId);
        if (targetUserId == null) {
            return Collections.emptyList();
        }
        refreshAchievements(targetUserId);
        List<Achievement> achievements = achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getStatus, 1)
                .orderByAsc(Achievement::getAchievementType, Achievement::getConditionValue));
        if (achievements.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserAchievement> userAchievementMap = userAchievementMapper.selectList(new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, targetUserId))
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, item -> item, (a, b) -> a));

        CheckSummary summary = buildCheckSummary(targetUserId);
        return achievements.stream()
                .map(achievement -> {
                    UserAchievement userAchievement = userAchievementMap.get(achievement.getId());
                    int progress = userAchievement != null ? safeInt(userAchievement.getProgress()) : calculateAchievementProgress(achievement, summary);
                    return UserAchievementVO.builder()
                            .id(userAchievement == null ? null : userAchievement.getId())
                            .achievementId(achievement.getId())
                            .achievementCode(achievement.getAchievementCode())
                            .achievementName(achievement.getAchievementName())
                            .achievementDesc(achievement.getAchievementDesc())
                            .achievementIcon(achievement.getAchievementIcon())
                            .achievementType(achievement.getAchievementType())
                            .conditionType(achievement.getConditionType())
                            .conditionValue(achievement.getConditionValue())
                            .pointReward(achievement.getPointReward())
                            .progress(progress)
                            .isClaimed(userAchievement == null ? 0 : safeInt(userAchievement.getIsClaimed()))
                            .achievedAt(userAchievement == null ? null : userAchievement.getAchievedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeRecord(Long checkId) {
        Long currentUserId = getCurrentUserId();
        getVisibleRecord(checkId, currentUserId);
        long existing = checkRecordLikeMapper.selectCount(new LambdaQueryWrapper<CheckRecordLike>()
                .eq(CheckRecordLike::getCheckId, checkId)
                .eq(CheckRecordLike::getUserId, currentUserId));
        if (existing > 0) {
            return;
        }
        CheckRecordLike like = new CheckRecordLike();
        like.setCheckId(checkId);
        like.setUserId(currentUserId);
        like.setCreatedAt(LocalDateTime.now());
        checkRecordLikeMapper.insert(like);
        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("like_count = IFNULL(like_count, 0) + 1"));
        log.info("点赞打卡记录成功, checkId={}, userId={}", checkId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeRecord(Long checkId) {
        Long currentUserId = getCurrentUserId();
        getVisibleRecord(checkId, currentUserId);
        CheckRecordLike like = checkRecordLikeMapper.selectOne(new LambdaQueryWrapper<CheckRecordLike>()
                .eq(CheckRecordLike::getCheckId, checkId)
                .eq(CheckRecordLike::getUserId, currentUserId)
                .last("limit 1"));
        if (like == null) {
            return;
        }
        checkRecordLikeMapper.deleteById(like.getId());
        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        log.info("取消点赞打卡记录成功, checkId={}, userId={}", checkId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long checkId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(checkId, commentId);
        long existing = checkCommentLikeMapper.selectCount(new LambdaQueryWrapper<CheckCommentLike>()
                .eq(CheckCommentLike::getCommentId, commentId)
                .eq(CheckCommentLike::getUserId, currentUserId));
        if (existing > 0) {
            return;
        }
        CheckCommentLike like = new CheckCommentLike();
        like.setCommentId(commentId);
        like.setUserId(currentUserId);
        like.setCreatedAt(LocalDateTime.now());
        checkCommentLikeMapper.insert(like);
        checkCommentMapper.update(null, new LambdaUpdateWrapper<CheckComment>()
                .eq(CheckComment::getId, commentId)
                .setSql("like_count = IFNULL(like_count, 0) + 1"));
        log.info("点赞打卡评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long checkId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(checkId, commentId);
        CheckCommentLike like = checkCommentLikeMapper.selectOne(new LambdaQueryWrapper<CheckCommentLike>()
                .eq(CheckCommentLike::getCommentId, commentId)
                .eq(CheckCommentLike::getUserId, currentUserId)
                .last("limit 1"));
        if (like == null) {
            return;
        }
        checkCommentLikeMapper.deleteById(like.getId());
        checkCommentMapper.update(null, new LambdaUpdateWrapper<CheckComment>()
                .eq(CheckComment::getId, commentId)
                .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        log.info("取消点赞打卡评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    private CheckComment requireVisibleComment(Long checkId, Long commentId) {
        CheckComment comment = checkCommentMapper.selectById(commentId);
        if (comment == null
                || !Objects.equals(comment.getCheckId(), checkId)
                || !Objects.equals(comment.getStatus(), COMMENT_STATUS_NORMAL)) {
            throw new CheckException("评论不存在");
        }
        return comment;
    }

    private Set<Long> loadLikedCommentIds(List<CheckComment> comments, Long currentUserId) {
        if (currentUserId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(CheckComment::getId).filter(Objects::nonNull).toList();
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return checkCommentLikeMapper.selectList(new LambdaQueryWrapper<CheckCommentLike>()
                        .eq(CheckCommentLike::getUserId, currentUserId)
                        .in(CheckCommentLike::getCommentId, commentIds))
                .stream()
                .map(CheckCommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareRecord(Long checkId) {
        getVisibleRecord(checkId, getCurrentUserIdNullable());
        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("share_count = IFNULL(share_count, 0) + 1"));
        log.info("分享打卡记录成功, checkId={}", checkId);
    }

    @Override
    public void claimAchievementReward(ClaimAchievementRewardRequest request) {

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckCommentVO createComment(Long checkId, CreateCheckCommentRequest request) {
        Long currentUserId = getCurrentUserId();
        CheckRecord record = getVisibleRecord(checkId, currentUserId);
        CheckComment parent = null;
        if (request.getParentId() != null && request.getParentId() > 0) {
            parent = checkCommentMapper.selectById(request.getParentId());
            if (parent == null || !Objects.equals(parent.getCheckId(), checkId)) {
                throw new CheckException("鐖惰瘎璁轰笉瀛樺湪");
            }
        }

        CheckComment comment = new CheckComment();
        comment.setCheckId(checkId);
        comment.setUserId(currentUserId);
        comment.setParentId(parent == null ? 0L : parent.getId());
        Long replyToUserId = request.getReplyToUserId() != null
                ? request.getReplyToUserId()
                : (parent == null ? null : parent.getUserId());
        comment.setReplyToUserId(replyToUserId);
        comment.setReplyCount(0);
        comment.setContent(request.getContent().trim());
        comment.setLikeCount(0);
        comment.setStatus(COMMENT_STATUS_NORMAL);
        comment.setCreatedAt(LocalDateTime.now());
        checkCommentMapper.insert(comment);

        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("comment_count = IFNULL(comment_count, 0) + 1"));

        if (parent != null) {
            checkCommentMapper.update(null, new LambdaUpdateWrapper<CheckComment>()
                    .eq(CheckComment::getId, parent.getId())
                    .setSql("reply_count = IFNULL(reply_count, 0) + 1"));
        }

        notifyCommentReply(record, comment, parent, currentUserId);
        log.info("创建打卡评论成功, checkId={}, commentId={}, userId={}, parentId={}",
                checkId, comment.getId(), currentUserId, comment.getParentId());
        return buildCommentVO(comment, loadUserProfiles(collectCommentUserIds(List.of(comment))), Collections.emptySet());
    }

    private void notifyCommentReply(CheckRecord record, CheckComment comment, CheckComment parent, Long currentUserId) {
        try {
            Set<Long> recipients = new LinkedHashSet<>();
            if (record.getUserId() != null && !Objects.equals(record.getUserId(), currentUserId)) {
                recipients.add(record.getUserId());
            }
            if (parent != null && parent.getUserId() != null && !Objects.equals(parent.getUserId(), currentUserId)) {
                recipients.add(parent.getUserId());
            }
            if (recipients.isEmpty()) {
                return;
            }
            UserProfileVO profile = fetchUserProfile(currentUserId);
            String fromNickname = resolveUserName(profile, currentUserId);
            String fromAvatar = resolveUserAvatar(profile);
            String preview = comment.getContent() == null ? null
                    : comment.getContent().substring(0, Math.min(200, comment.getContent().length()));
            List<CommentReplyNotifyRequest> requests = recipients.stream()
                    .map(toUserId -> {
                        CommentReplyNotifyRequest notify = new CommentReplyNotifyRequest();
                        notify.setSourceType("check");
                        notify.setSourceId(record.getId());
                        notify.setSourceTitle(record.getTitle());
                        notify.setCommentId(comment.getId());
                        notify.setParentCommentId(comment.getParentId());
                        notify.setFromUserId(currentUserId);
                        notify.setFromNickname(fromNickname);
                        notify.setFromAvatar(fromAvatar);
                        notify.setToUserId(toUserId);
                        notify.setContentPreview(preview);
                        return notify;
                    })
                    .toList();
            sendCommentReplyNotifications(requests);
        } catch (Exception e) {
            log.warn("构建评论回复通知失败, checkId={}, commentId={}", record.getId(), comment.getId(), e);
        }
    }

    private void sendCommentReplyNotifications(List<CommentReplyNotifyRequest> requests) {
        Runnable task = () -> requests.forEach(notify -> {
            try {
                messageFeignClient.notifyCommentReply(notify);
            } catch (Exception e) {
                log.warn("发送评论回复通知失败, toUserId={}", notify.getToUserId(), e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Override
    public List<CheckCommentVO> listComments(Long checkId) {
        Long currentUserId = getCurrentUserIdNullable();
        getVisibleRecord(checkId, currentUserId);
        List<CheckComment> comments = checkCommentMapper.selectList(new LambdaQueryWrapper<CheckComment>()
                .eq(CheckComment::getCheckId, checkId)
                .eq(CheckComment::getStatus, COMMENT_STATUS_NORMAL)
                .orderByAsc(CheckComment::getCreatedAt));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(collectCommentUserIds(comments));
        Set<Long> likedCommentIds = loadLikedCommentIds(comments, currentUserId);
        Map<Long, List<CheckCommentVO>> childrenMap = new LinkedHashMap<>();
        List<CheckCommentVO> roots = new ArrayList<>();
        for (CheckComment comment : comments) {
            CheckCommentVO vo = buildCommentVO(comment, userProfiles, likedCommentIds);
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                roots.add(vo);
            } else {
                childrenMap.computeIfAbsent(comment.getParentId(), key -> new ArrayList<>()).add(vo);
            }
        }
        roots.forEach(root -> attachChildrenRecursively(root, childrenMap));
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long checkId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        CheckComment comment = checkCommentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getCheckId(), checkId)) {
            throw new CheckException("评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), currentUserId) && !isAdmin()) {
            throw new CheckException("无权删除该评论");
        }
        if (!Objects.equals(comment.getStatus(), COMMENT_STATUS_NORMAL)) {
            return;
        }
        comment.setStatus(0);
        checkCommentMapper.updateById(comment);
        checkRecordMapper.update(null, new LambdaUpdateWrapper<CheckRecord>()
                .eq(CheckRecord::getId, checkId)
                .setSql("comment_count = GREATEST(IFNULL(comment_count, 0) - 1, 0)"));
        log.info("删除打卡评论成功, checkId={}, commentId={}, userId={}", checkId, commentId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckGroupVO createGroup(CreateCheckGroupRequest request) {
        Long currentUserId = getCurrentUserId();
        CheckGroup group = new CheckGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setGroupDesc(normalizeText(request.getGroupDesc(), 200));
        group.setCoverImage(normalizeText(request.getCoverImage(), 255));
        group.setCreatorId(currentUserId);
        group.setMemberCount(1);
        group.setArticleCount(0);
        group.setIsPublic(normalizePublicFlag(request.getIsPublic()));
        group.setStatus(GROUP_STATUS_NORMAL);
        group.setCreatedAt(LocalDateTime.now());
        checkGroupMapper.insert(group);

        CheckGroupMember owner = new CheckGroupMember();
        owner.setGroupId(group.getId());
        owner.setUserId(currentUserId);
        owner.setRole(MEMBER_ROLE_OWNER);
        owner.setJoinedAt(LocalDateTime.now());
        owner.setStatus(MEMBER_STATUS_NORMAL);
        checkGroupMemberMapper.insert(owner);
        log.info("创建打卡小组成功, groupId={}, creatorId={}", group.getId(), currentUserId);
        return toGroupVO(group, loadUserProfiles(Set.of(currentUserId)));
    }

    @Override
    public List<CheckGroupVO> listPublicGroups() {
        List<CheckGroup> groups = checkGroupMapper.selectList(new LambdaQueryWrapper<CheckGroup>()
                .eq(CheckGroup::getStatus, GROUP_STATUS_NORMAL)
                .eq(CheckGroup::getIsPublic, 1)
                .orderByDesc(CheckGroup::getMemberCount, CheckGroup::getCreatedAt));
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(groups.stream().map(CheckGroup::getCreatorId).collect(Collectors.toSet()));
        return groups.stream().map(group -> toGroupVO(group, userProfiles)).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinGroup(Long groupId) {
        Long currentUserId = getCurrentUserId();
        CheckGroup group = checkGroupMapper.selectById(groupId);
        if (group == null || !Objects.equals(group.getStatus(), GROUP_STATUS_NORMAL)) {
            throw new CheckException("打卡小组不存在");
        }
        CheckGroupMember existed = checkGroupMemberMapper.selectOne(new LambdaQueryWrapper<CheckGroupMember>()
                .eq(CheckGroupMember::getGroupId, groupId)
                .eq(CheckGroupMember::getUserId, currentUserId)
                .last("limit 1"));
        if (existed != null && Objects.equals(existed.getStatus(), MEMBER_STATUS_NORMAL)) {
            return;
        }
        if (existed == null) {
            existed = new CheckGroupMember();
            existed.setGroupId(groupId);
            existed.setUserId(currentUserId);
            existed.setRole(1);
            existed.setJoinedAt(LocalDateTime.now());
            existed.setStatus(MEMBER_STATUS_NORMAL);
            checkGroupMemberMapper.insert(existed);
        } else {
            existed.setStatus(MEMBER_STATUS_NORMAL);
            existed.setJoinedAt(LocalDateTime.now());
            checkGroupMemberMapper.updateById(existed);
        }
        checkGroupMapper.update(null, new LambdaUpdateWrapper<CheckGroup>()
                .eq(CheckGroup::getId, groupId)
                .setSql("member_count = IFNULL(member_count, 0) + 1"));
        log.info("加入打卡小组成功, groupId={}, userId={}", groupId, currentUserId);
    }

    @Override
    public void quitGroup(Long groupId) {

    }

    @Override
    public void removeGroupMember(Long groupId, OperateGroupMemberRequest request) {

    }

    private CheckRecord getOwnedRecord(Long checkId, Long currentUserId) {
        CheckRecord record = checkRecordMapper.selectById(checkId);
        if (record == null || Objects.equals(record.getStatus(), STATUS_DELETED)) {
            throw new CheckException("打卡记录不存在");
        }
        if (!Objects.equals(record.getUserId(), currentUserId) && !isAdmin()) {
            throw new CheckException("无权操作该打卡记录");
        }
        return record;
    }

    private Long resolveTargetUserId(Long userId) {
        Long currentUserId = getCurrentUserIdNullable();
        if (userId == null) {
            // Guests have no identity: callers fall back to empty statistics.
            return currentUserId;
        }
        if (currentUserId != null && !Objects.equals(userId, currentUserId) && !isAdmin()) {
            throw new CheckException("无权查看该用户统计");
        }
        return userId;
    }

    private boolean isAdmin() {
        String roles = servletRequest.getHeader("X-User-Roles");
        if (!StringUtils.hasText(roles)) {
            return false;
        }
        return List.of(roles.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .anyMatch(role -> "ADMIN".equals(role)
                        || "ROLE_ADMIN".equals(role)
                        || "SUPER_ADMIN".equals(role)
                        || "MANAGER".equals(role));
    }

    private CheckRecord getVisibleRecord(Long checkId, Long currentUserId) {
        CheckRecord record = checkRecordMapper.selectById(checkId);
        if (record == null || Objects.equals(record.getStatus(), STATUS_DELETED)) {
            throw new CheckException("打卡记录不存在");
        }
        if (Objects.equals(record.getStatus(), STATUS_PRIVATE) && !Objects.equals(record.getUserId(), currentUserId)) {
            throw new CheckException("该打卡仅自己可见");
        }
        if (Objects.equals(record.getStatus(), STATUS_DRAFT) && !Objects.equals(record.getUserId(), currentUserId)) {
            throw new CheckException("草稿不可查看");
        }
        return record;
    }

    private void ensureUniqueCheckDate(Long userId, LocalDate checkDate, Long excludeId) {
        LambdaQueryWrapper<CheckRecord> wrapper = new LambdaQueryWrapper<CheckRecord>()
                .eq(CheckRecord::getUserId, userId)
                .eq(CheckRecord::getCheckDate, checkDate)
                .ne(CheckRecord::getStatus, STATUS_DELETED);
        if (excludeId != null) {
            wrapper.ne(CheckRecord::getId, excludeId);
        }
        Long count = checkRecordMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new CheckException("当天已经打过卡，不能重复创建");
        }
    }

    private void validateCheckDate(LocalDate checkDate) {
        LocalDate today = LocalDate.now();
        if (checkDate.isAfter(today)) {
            throw new CheckException("打卡日期不能晚于今天");
        }
        if (checkDate.isBefore(today.minusYears(1))) {
            throw new CheckException("仅支持补签一年内的打卡记录");
        }
    }

    private void refreshMonthlyStatistic(Long userId, int year, int month) {
        List<CheckRecord> monthlyRecords = checkRecordMapper.selectList(new LambdaQueryWrapper<CheckRecord>()
                .eq(CheckRecord::getUserId, userId)
                .eq(CheckRecord::getStatus, STATUS_PUBLISHED)
                .apply("YEAR(check_date) = {0}", year)
                .apply("MONTH(check_date) = {0}", month)
                .orderByAsc(CheckRecord::getCheckDate));
        CheckStatistic statistic = checkStatisticMapper.selectOne(new LambdaQueryWrapper<CheckStatistic>()
                .eq(CheckStatistic::getUserId, userId)
                .eq(CheckStatistic::getCheckYear, year)
                .eq(CheckStatistic::getCheckMonth, month)
                .last("limit 1"));
        if (monthlyRecords.isEmpty()) {
            if (statistic != null) {
                checkStatisticMapper.deleteById(statistic.getId());
            }
            return;
        }

        List<LocalDate> dates = monthlyRecords.stream().map(CheckRecord::getCheckDate).distinct().sorted().toList();
        int maxContinuousDays = calculateMaxContinuousDays(dates);
        LocalDate lastCheckDate = dates.get(dates.size() - 1);
        int continuousDays = calculateCurrentContinuousDays(dates, lastCheckDate);
        BigDecimal totalLearnHours = monthlyRecords.stream()
                .map(CheckRecord::getLearnHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int totalLikes = monthlyRecords.stream().mapToInt(item -> safeInt(item.getLikeCount())).sum();

        if (statistic == null) {
            statistic = new CheckStatistic();
            statistic.setUserId(userId);
            statistic.setCheckYear(year);
            statistic.setCheckMonth(month);
        }
        statistic.setTotalDays(dates.size());
        statistic.setContinuousDays(continuousDays);
        statistic.setMaxContinuousDays(maxContinuousDays);
        statistic.setTotalArticles(monthlyRecords.size());
        statistic.setTotalLikesReceived(totalLikes);
        statistic.setTotalLearnHours(totalLearnHours);
        statistic.setLastCheckDate(lastCheckDate);
        statistic.setUpdatedAt(LocalDateTime.now());

        if (statistic.getId() == null) {
            checkStatisticMapper.insert(statistic);
        } else {
            checkStatisticMapper.updateById(statistic);
        }
    }

    private void refreshAchievements(Long userId) {
        List<Achievement> achievements = achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getStatus, 1)
                .eq(Achievement::getAchievementType, ACHIEVEMENT_TYPE_CHECK));
        if (achievements.isEmpty()) {
            return;
        }
        CheckSummary summary = buildCheckSummary(userId);
        Map<Long, UserAchievement> existingMap = userAchievementMapper.selectList(new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, item -> item, (a, b) -> a));

        for (Achievement achievement : achievements) {
            int progress = calculateAchievementProgress(achievement, summary);
            UserAchievement existing = existingMap.get(achievement.getId());
            if (progress >= safeInt(achievement.getConditionValue())) {
                if (existing == null) {
                    UserAchievement userAchievement = new UserAchievement();
                    userAchievement.setUserId(userId);
                    userAchievement.setAchievementId(achievement.getId());
                    userAchievement.setAchievedAt(LocalDateTime.now());
                    userAchievement.setProgress(progress);
                    userAchievement.setIsClaimed(0);
                    userAchievementMapper.insert(userAchievement);
                } else {
                    existing.setProgress(progress);
                    userAchievementMapper.updateById(existing);
                }
            }
        }
    }

    private CheckSummary buildCheckSummary(Long userId) {
        List<CheckRecord> records = checkRecordMapper.selectList(new LambdaQueryWrapper<CheckRecord>()
                .eq(CheckRecord::getUserId, userId)
                .eq(CheckRecord::getStatus, STATUS_PUBLISHED)
                .orderByAsc(CheckRecord::getCheckDate));
        List<LocalDate> dates = records.stream().map(CheckRecord::getCheckDate).distinct().sorted().toList();
        BigDecimal totalLearnHours = records.stream()
                .map(CheckRecord::getLearnHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return CheckSummary.builder()
                .totalDays(dates.size())
                .continuousDays(dates.isEmpty() ? 0 : calculateCurrentContinuousDays(dates, dates.get(dates.size() - 1)))
                .maxContinuousDays(calculateMaxContinuousDays(dates))
                .totalArticles(records.size())
                .totalLikes(records.stream().mapToInt(item -> safeInt(item.getLikeCount())).sum())
                .totalLearnHours(totalLearnHours)
                .build();
    }

    private int calculateAchievementProgress(Achievement achievement, CheckSummary summary) {
        return switch (safeInt(achievement.getConditionType())) {
            case CONDITION_TYPE_DAYS -> summary.getTotalDays();
            case CONDITION_TYPE_COUNT -> summary.getTotalArticles();
            case CONDITION_TYPE_CONTINUOUS -> summary.getMaxContinuousDays();
            default -> 0;
        };
    }

    private int calculateMaxContinuousDays(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        int max = 1;
        int current = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).plusDays(1).equals(dates.get(i))) {
                current++;
            } else {
                current = 1;
            }
            max = Math.max(max, current);
        }
        return max;
    }

    private int calculateCurrentContinuousDays(List<LocalDate> dates, LocalDate lastDate) {
        if (dates.isEmpty()) {
            return 0;
        }
        int current = 1;
        for (int i = dates.size() - 1; i > 0; i--) {
            if (dates.get(i - 1).plusDays(1).equals(dates.get(i))) {
                current++;
            } else {
                break;
            }
        }
        return Objects.equals(lastDate, dates.get(dates.size() - 1)) ? current : 0;
    }

    private CheckRecordVO toRecordDetailVO(CheckRecord record, Long currentUserId) {
        UserProfileVO userProfile = fetchUserProfile(record.getUserId());
        return CheckRecordVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .username(resolveUserName(userProfile, record.getUserId()))
                .avatarUrl(resolveUserAvatar(userProfile))
                .schoolName(resolveSchoolName(userProfile))
                .companyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .title(record.getTitle())
                .summary(record.getSummary())
                .content(record.getContent())
                .imageUrls(parseImageUrls(record.getImageUrls()))
                .learnHours(normalizeLearnHours(record.getLearnHours()))
                .mood(record.getMood())
                .location(record.getLocation())
                .viewCount(safeInt(record.getViewCount()))
                .likeCount(safeInt(record.getLikeCount()))
                .commentCount(safeInt(record.getCommentCount()))
                .shareCount(safeInt(record.getShareCount()))
                .isTop(safeInt(record.getIsTop()))
                .isFeatured(safeInt(record.getIsFeatured()))
                .status(record.getStatus())
                .checkDate(record.getCheckDate())
                .checkTime(record.getCheckTime())
                .liked(false)
                .canEdit(Objects.equals(record.getUserId(), currentUserId))
                .build();
    }

    private CheckStatisticsVO toStatisticsVO(CheckStatistic statistic) {
        return CheckStatisticsVO.builder()
                .userId(statistic.getUserId())
                .year(statistic.getCheckYear())
                .month(statistic.getCheckMonth())
                .totalDays(safeInt(statistic.getTotalDays()))
                .continuousDays(safeInt(statistic.getContinuousDays()))
                .maxContinuousDays(safeInt(statistic.getMaxContinuousDays()))
                .totalArticles(safeInt(statistic.getTotalArticles()))
                .totalLikesReceived(safeInt(statistic.getTotalLikesReceived()))
                .totalLearnHours(statistic.getTotalLearnHours() == null
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : statistic.getTotalLearnHours().setScale(2, RoundingMode.HALF_UP))
                .lastCheckDate(statistic.getLastCheckDate())
                .build();
    }

    private CheckCommentVO buildCommentVO(CheckComment comment, Map<Long, UserProfileVO> userProfiles, Set<Long> likedCommentIds) {
        UserProfileVO userProfile = userProfiles.get(comment.getUserId());
        UserProfileVO replyToUserProfile = userProfiles.get(comment.getReplyToUserId());
        return CheckCommentVO.builder()
                .id(comment.getId())
                .checkId(comment.getCheckId())
                .userId(comment.getUserId())
                .username(resolveUserName(userProfile, comment.getUserId()))
                .userAvatar(resolveUserAvatar(userProfile))
                .schoolName(resolveSchoolName(userProfile))
                .companyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .parentId(comment.getParentId())
                .replyToUserId(comment.getReplyToUserId())
                .replyToUsername(comment.getReplyToUserId() == null ? null : resolveUserName(replyToUserProfile, comment.getReplyToUserId()))
                .content(comment.getContent())
                .likeCount(safeInt(comment.getLikeCount()))
                .liked(likedCommentIds.contains(comment.getId()))
                .createdAt(comment.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }

    private void attachChildrenRecursively(CheckCommentVO root, Map<Long, List<CheckCommentVO>> childrenMap) {
        List<CheckCommentVO> children = childrenMap.getOrDefault(root.getId(), Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(CheckCommentVO::getCreatedAt))
                .collect(Collectors.toList());
        root.setChildren(children);
        children.forEach(child -> attachChildrenRecursively(child, childrenMap));
    }

    private CheckGroupVO toGroupVO(CheckGroup group, Map<Long, UserProfileVO> userProfiles) {
        UserProfileVO userProfile = userProfiles.get(group.getCreatorId());
        return CheckGroupVO.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .groupDesc(group.getGroupDesc())
                .coverImage(group.getCoverImage())
                .creatorId(group.getCreatorId())
                .creatorName(resolveUserName(userProfile, group.getCreatorId()))
                .creatorAvatar(resolveUserAvatar(userProfile))
                .creatorSchoolName(resolveSchoolName(userProfile))
                .creatorCompanyName(resolveCompanyName(userProfile))
                .creatorTitle(resolveTitle(userProfile))
                .memberCount(safeInt(group.getMemberCount()))
                .articleCount(safeInt(group.getArticleCount()))
                .isPublic(safeInt(group.getIsPublic()))
                .status(safeInt(group.getStatus()))
                .build();
    }

    private void increaseActivityLevel(Long userId) {
        ApiResponse<Void> response = userFeignClient.increaseActivityLevel(userId);
        if (response == null || response.getCode() == null || response.getCode() != 0) {
            throw new CheckException(response == null ? "更新用户活跃度失败" : response.getMessage());
        }
    }

    private Map<Long, UserProfileVO> loadUserProfiles(Set<Long> userIds) {
        Map<Long, UserProfileVO> result = new HashMap<>();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            UserProfileVO userProfile = fetchUserProfile(userId);
            if (userProfile != null) {
                result.put(userId, userProfile);
            }
        }
        return result;
    }

    private UserProfileVO fetchUserProfile(Long userId) {
        try {
            ApiResponse<UserProfileVO> response = userFeignClient.getUserProfile(userId);
            return response == null ? null : response.getData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUserName(UserProfileVO userProfile, Long userId) {
        if (userProfile != null && StringUtils.hasText(userProfile.getNickname())) {
            return userProfile.getNickname();
        }
        if (userProfile != null && StringUtils.hasText(userProfile.getUsername())) {
            return userProfile.getUsername();
        }
        return "用户" + userId;
    }

    private String resolveUserAvatar(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getAvatarUrl();
    }

    private String resolveSchoolName(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getSchoolName();
    }

    private String resolveCompanyName(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getCompanyName();
    }

    private String resolveTitle(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getTitle();
    }

    private Integer resolveAuthorIsVip(UserProfileVO userProfile) {
        if (userProfile == null || !Objects.equals(userProfile.getIsVip(), 1)
                || userProfile.getVipExpiredAt() == null) {
            return 0;
        }
        return userProfile.getVipExpiredAt().isAfter(LocalDateTime.now()) ? 1 : 0;
    }

    private Set<Long> collectCommentUserIds(List<CheckComment> comments) {
        return comments.stream()
                .flatMap(comment -> java.util.stream.Stream.of(comment.getUserId(), comment.getReplyToUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String toJsonArray(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "[]";
        }
        List<String> normalized = imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(item -> limitLength(item, 255))
                .toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new CheckException("鍥剧墖鏁版嵁鏍煎紡閿欒");
        }
    }

    private List<String> parseImageUrls(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imageUrls, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private BigDecimal normalizeLearnHours(BigDecimal learnHours) {
        if (learnHours == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (learnHours.compareTo(BigDecimal.ZERO) < 0 || learnHours.compareTo(new BigDecimal("24")) > 0) {
            throw new CheckException("学习时长必须在0到24小时之间");
        }
        return learnHours.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizeMood(Integer mood) {
        if (mood == null) {
            return null;
        }
        if (mood < 1 || mood > 5) {
            throw new CheckException("心情值必须在1到5之间");
        }
        return mood;
    }

    private Integer normalizeRecordStatus(Integer status) {
        if (status == null) {
            return STATUS_PUBLISHED;
        }
        if (!List.of(STATUS_PUBLISHED, STATUS_DRAFT, STATUS_PRIVATE).contains(status)) {
            throw new CheckException("打卡状态不合法");
        }
        return status;
    }

    private Integer normalizePublicFlag(Integer isPublic) {
        if (isPublic == null) {
            return 1;
        }
        if (!Objects.equals(isPublic, 0) && !Objects.equals(isPublic, 1)) {
            throw new CheckException("小组公开状态不合法");
        }
        return isPublic;
    }

    private boolean shouldSkipViewCount() {
        return "development".equalsIgnoreCase(servletRequest.getHeader("X-Client-Env"));
    }

    private String normalizeText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return limitLength(text.trim(), maxLength);
    }

    private String limitLength(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private Long getCurrentUserId() {
        String userId = servletRequest.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new CheckException("未登录或登录已失效");
        }
        return Long.parseLong(userId);
    }

    private Long getCurrentUserIdNullable() {
        String userId = servletRequest.getHeader("X-User-Id");
        return StringUtils.hasText(userId) ? Long.parseLong(userId) : null;
    }

    private String getClientIp() {
        String forwarded = servletRequest.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return servletRequest.getRemoteAddr();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @lombok.Data
    @lombok.Builder
    private static class CheckSummary {
        private int totalDays;
        private int continuousDays;
        private int maxContinuousDays;
        private int totalArticles;
        private int totalLikes;
        private BigDecimal totalLearnHours;
    }
}


