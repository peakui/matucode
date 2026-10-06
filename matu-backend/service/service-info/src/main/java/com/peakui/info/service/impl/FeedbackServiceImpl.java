package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.common.result.PageResponse;
import com.peakui.info.exception.InfoException;
import com.peakui.info.feign.AuthUserFeignClient;
import com.peakui.info.mapper.UserFeedbackMapper;
import com.peakui.info.mapper.FeedbackHistoryMapper;
import com.peakui.info.model.entity.FeedbackHistory;
import com.peakui.info.model.dto.CreateFeedbackRequest;
import com.peakui.info.model.dto.FeedbackAttachmentItem;
import com.peakui.info.model.dto.UpdateFeedbackRequest;
import com.peakui.info.model.entity.UserFeedback;
import com.peakui.info.model.vo.FeedbackDetailVO;
import com.peakui.info.model.vo.FeedbackListItemVO;
import com.peakui.info.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final TypeReference<List<FeedbackAttachmentItem>> ATTACHMENT_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> EXTRA_INFO_TYPE = new TypeReference<>() {};

    private final UserFeedbackMapper userFeedbackMapper;
    private final FeedbackHistoryMapper feedbackHistoryMapper;
    private final AuthUserFeignClient authUserFeignClient;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO createFeedback(CreateFeedbackRequest request) {
        Long currentUserId = currentUserIdIfLogin();
        AuthUserFeignClient.UserProfileResponse userProfile = currentUserId == null ? null : fetchUserProfile(currentUserId);

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(currentUserId);
        feedback.setUsername(resolveUsername(request.getContactEmail(), userProfile));
        feedback.setContactEmail(resolveContactEmail(request.getContactEmail(), userProfile));
        feedback.setType(request.getType() == null ? 0 : request.getType());
        feedback.setTitle(request.getTitle().trim());
        feedback.setContent(request.getContent());
        feedback.setAttachments(toJson(request.getAttachments()));
        feedback.setExtraInfo(toJson(request.getExtraInfo()));
        feedback.setStatus(0);
        feedback.setPriority(1);
        feedback.setIpAddress(resolveClientIp());
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        userFeedbackMapper.insert(feedback);
        log.info("提交反馈成功, feedbackId={}, userId={}, type={}, title={}",
                feedback.getId(), currentUserId, feedback.getType(), feedback.getTitle());
        return toDetailVO(feedback);
    }

    @Override
    public PageResponse<FeedbackListItemVO> listMyFeedbacks(Long pageNum, Long pageSize) {
        Long currentUserId = requireLoginUserId();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<UserFeedback>()
                .eq(UserFeedback::getUserId, currentUserId)
                .orderByAsc(UserFeedback::getStatus)
                .orderByDesc(UserFeedback::getPriority, UserFeedback::getCreatedAt);
        Page<UserFeedback> page = userFeedbackMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toListItemVO).toList());
    }

    @Override
    public FeedbackDetailVO getFeedbackDetail(Long id) {
        UserFeedback feedback = getFeedback(id);
        Long currentUserId = requireLoginUserId();
        if (!isAdmin() && !Objects.equals(currentUserId, feedback.getUserId())) {
            throw new InfoException("无权限查看该反馈");
        }
        return toDetailVO(feedback);
    }

    @Override
    public PageResponse<FeedbackListItemVO> listAdminFeedbacks(Integer type, Integer status, Integer priority,
                                                               Long assigneeId, Long userId, String keyword,
                                                               Long pageNum, Long pageSize) {
        checkAdmin();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);

        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<UserFeedback>();
        if (type != null) {
            wrapper.eq(UserFeedback::getType, type);
        }
        if (status != null) {
            wrapper.eq(UserFeedback::getStatus, status);
        }
        if (priority != null) {
            wrapper.eq(UserFeedback::getPriority, priority);
        }
        if (assigneeId != null) {
            wrapper.eq(UserFeedback::getAssigneeId, assigneeId);
        }
        if (userId != null) {
            wrapper.eq(UserFeedback::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UserFeedback::getTitle, keyword).or().like(UserFeedback::getContent, keyword));
        }
        wrapper.orderByAsc(UserFeedback::getStatus).orderByDesc(UserFeedback::getPriority, UserFeedback::getCreatedAt);

        Page<UserFeedback> page = userFeedbackMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toListItemVO).toList());
    }

    @Override
    public FeedbackDetailVO getAdminFeedbackDetail(Long id) {
        checkAdmin();
        return toDetailVO(getFeedback(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackDetailVO updateFeedback(Long id, UpdateFeedbackRequest request) {
        checkAdmin();
        UserFeedback feedback = userFeedbackMapper.lockById(id);
        if (feedback == null) throw new InfoException("反馈不存在");
        Integer previousStatus = feedback.getStatus();
        boolean replyChanged = request.getReplyContent() != null && !Objects.equals(request.getReplyContent(), feedback.getReplyContent());
        boolean statusChanged = request.getStatus() != null && !Objects.equals(previousStatus, request.getStatus());
        boolean changed = replyChanged || statusChanged
                || (request.getPriority() != null && !Objects.equals(request.getPriority(), feedback.getPriority()))
                || (request.getAssigneeId() != null && !Objects.equals(request.getAssigneeId(), feedback.getAssigneeId()));
        if (!changed) return toDetailVO(feedback);

        if (request.getStatus() != null) {
            feedback.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            feedback.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            feedback.setAssigneeId(request.getAssigneeId());
        }
        if (request.getReplyContent() != null) {
            feedback.setReplyContent(request.getReplyContent());
        }
        if (replyChanged) {
            feedback.setRepliedAt(StringUtils.hasText(request.getReplyContent()) ? LocalDateTime.now() : null);
        }
        if (statusChanged) {
            feedback.setResolvedAt(Objects.equals(feedback.getStatus(), 2) ? LocalDateTime.now() : null);
        }
        feedback.setUpdatedAt(LocalDateTime.now());
        userFeedbackMapper.updateById(feedback);
        FeedbackHistory history = new FeedbackHistory();
        history.setFeedbackId(id);
        history.setOperatorId(StpUtil.getLoginIdAsLong());
        history.setFromStatus(previousStatus);
        history.setToStatus(feedback.getStatus());
        history.setPriority(feedback.getPriority());
        history.setAssigneeId(feedback.getAssigneeId());
        history.setReplyContent(replyChanged ? feedback.getReplyContent() : null);
        history.setCreatedAt(feedback.getUpdatedAt());
        feedbackHistoryMapper.insert(history);
        log.info("更新反馈成功, feedbackId={}, status={}, priority={}, replyChanged={}",
                id, feedback.getStatus(), feedback.getPriority(), replyChanged);
        return toDetailVO(feedback);
    }

    private UserFeedback getFeedback(Long id) {
        UserFeedback feedback = userFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new InfoException("反馈不存在");
        }
        return feedback;
    }

    private Long currentUserIdIfLogin() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }

    private Long requireLoginUserId() {
        StpUtil.checkLogin();
        return StpUtil.getLoginIdAsLong();
    }

    private AuthUserFeignClient.UserProfileResponse fetchUserProfile(Long userId) {
        try {
            var response = authUserFeignClient.getUserProfile(userId);
            return response == null ? null : response.getData();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername(String contactEmail, AuthUserFeignClient.UserProfileResponse userProfile) {
        if (userProfile == null) {
            return null;
        }
        if (StringUtils.hasText(userProfile.getUsername())) {
            return userProfile.getUsername().trim();
        }
        if (StringUtils.hasText(userProfile.getNickname())) {
            return userProfile.getNickname().trim();
        }
        return null;
    }

    private String resolveContactEmail(String requestEmail, AuthUserFeignClient.UserProfileResponse userProfile) {
        if (StringUtils.hasText(requestEmail)) {
            return requestEmail.trim();
        }
        if (userProfile != null && StringUtils.hasText(userProfile.getEmail())) {
            return userProfile.getEmail().trim();
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new InfoException("反馈数据格式不合法");
        }
    }

    private List<FeedbackAttachmentItem> toAttachments(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, ATTACHMENT_LIST_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> toExtraInfo(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, EXTRA_INFO_TYPE);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private FeedbackListItemVO toListItemVO(UserFeedback feedback) {
        return FeedbackListItemVO.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .username(feedback.getUsername())
                .contactEmail(feedback.getContactEmail())
                .type(feedback.getType())
                .title(feedback.getTitle())
                .status(feedback.getStatus())
                .priority(feedback.getPriority())
                .assigneeId(feedback.getAssigneeId())
                .repliedAt(feedback.getRepliedAt())
                .resolvedAt(feedback.getResolvedAt())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private FeedbackDetailVO toDetailVO(UserFeedback feedback) {
        return FeedbackDetailVO.builder()
                .history(feedbackHistoryMapper.selectList(new LambdaQueryWrapper<FeedbackHistory>()
                        .eq(FeedbackHistory::getFeedbackId, feedback.getId())
                        .orderByDesc(FeedbackHistory::getCreatedAt, FeedbackHistory::getId)))
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .username(feedback.getUsername())
                .contactEmail(feedback.getContactEmail())
                .type(feedback.getType())
                .title(feedback.getTitle())
                .content(feedback.getContent())
                .attachments(toAttachments(feedback.getAttachments()))
                .extraInfo(toExtraInfo(feedback.getExtraInfo()))
                .status(feedback.getStatus())
                .priority(feedback.getPriority())
                .assigneeId(feedback.getAssigneeId())
                .replyContent(feedback.getReplyContent())
                .repliedAt(feedback.getRepliedAt())
                .resolvedAt(feedback.getResolvedAt())
                .ipAddress(feedback.getIpAddress())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private String resolveClientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private void checkAdmin() {
        StpUtil.checkLogin();
        StpUtil.checkRole("ADMIN");
    }

    private boolean isAdmin() {
        try {
            StpUtil.checkRole("ADMIN");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
