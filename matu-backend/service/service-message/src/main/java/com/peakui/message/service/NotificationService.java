package com.peakui.message.service;

import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.common.result.PageResponse;
import com.peakui.message.model.dto.MarkNotificationsReadRequest;
import com.peakui.message.model.vo.NotificationVO;

public interface NotificationService {

    void notifyCommentReply(CommentReplyNotifyRequest request);

    PageResponse<NotificationVO> listNotifications(Long pageNum, Long pageSize);

    long unreadCount();

    void markRead(MarkNotificationsReadRequest request);
}
