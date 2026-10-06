package com.peakui.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.common.result.PageResponse;
import com.peakui.message.exception.MessageException;
import com.peakui.message.mapper.NotificationMapper;
import com.peakui.message.model.dto.MarkNotificationsReadRequest;
import com.peakui.message.model.entity.Notification;
import com.peakui.message.model.vo.MessagePushEventVO;
import com.peakui.message.model.vo.NotificationVO;
import com.peakui.message.service.MessagePushService;
import com.peakui.message.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TYPE_COMMENT_REPLY = "comment_reply";
    private static final String EVENT_COMMENT_REPLY = "COMMENT_REPLY";
    private static final int READ_NO = 0;

    private final NotificationMapper notificationMapper;
    private final MessagePushService messagePushService;
    private final HttpServletRequest servletRequest;

    @Override
    public void notifyCommentReply(CommentReplyNotifyRequest request) {
        if (request == null || request.getToUserId() == null
                || request.getToUserId().equals(request.getFromUserId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setUserId(request.getToUserId());
        notification.setType(TYPE_COMMENT_REPLY);
        notification.setSourceType(request.getSourceType());
        notification.setSourceId(request.getSourceId());
        notification.setSourceTitle(request.getSourceTitle());
        notification.setCommentId(request.getCommentId());
        notification.setParentCommentId(request.getParentCommentId());
        notification.setFromUserId(request.getFromUserId());
        notification.setFromNickname(request.getFromNickname());
        notification.setFromAvatar(request.getFromAvatar());
        notification.setContentPreview(request.getContentPreview());
        notification.setIsRead(READ_NO);
        notification.setCreatedAt(now);
        notificationMapper.insert(notification);

        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notification.getId());
        data.put("sourceType", request.getSourceType());
        data.put("sourceId", request.getSourceId());
        data.put("sourceTitle", request.getSourceTitle());
        data.put("commentId", request.getCommentId());
        data.put("fromUserId", request.getFromUserId());
        data.put("fromNickname", request.getFromNickname());
        data.put("fromAvatar", request.getFromAvatar());
        data.put("contentPreview", request.getContentPreview());
        data.put("createdAt", now);
        MessagePushEventVO event = MessagePushEventVO.builder()
                .type(EVENT_COMMENT_REPLY)
                .senderId(request.getFromUserId())
                .receiverUserIds(List.of(request.getToUserId()))
                .data(data)
                .timestamp(now)
                .build();
        messagePushService.pushToUsers(List.of(request.getToUserId()), event);
        log.info("评论回复通知已推送, toUserId={}, sourceType={}, sourceId={}",
                request.getToUserId(), request.getSourceType(), request.getSourceId());
    }

    @Override
    public PageResponse<NotificationVO> listNotifications(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, currentUserId)
                        .orderByDesc(Notification::getCreatedAt));
        List<NotificationVO> items = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResponse.of(currentPage, currentSize, page.getTotal(), items);
    }

    @Override
    public long unreadCount() {
        Long currentUserId = getCurrentUserId();
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, currentUserId)
                .eq(Notification::getIsRead, READ_NO));
    }

    @Override
    public void markRead(MarkNotificationsReadRequest request) {
        Long currentUserId = getCurrentUserId();
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, currentUserId)
                .set(Notification::getIsRead, 1);
        if (request == null || Boolean.TRUE.equals(request.getAll())) {
            wrapper.eq(Notification::getIsRead, READ_NO);
            notificationMapper.update(null, wrapper);
            return;
        }
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return;
        }
        wrapper.in(Notification::getId, request.getIds());
        notificationMapper.update(null, wrapper);
    }

    private NotificationVO toVO(Notification notification) {
        return NotificationVO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .sourceTitle(notification.getSourceTitle())
                .commentId(notification.getCommentId())
                .fromUserId(notification.getFromUserId())
                .fromNickname(notification.getFromNickname())
                .fromAvatar(notification.getFromAvatar())
                .contentPreview(notification.getContentPreview())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Long getCurrentUserId() {
        String userId = servletRequest.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new MessageException("未登录或登录已失效");
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new MessageException("当前登录用户无效");
        }
    }
}
