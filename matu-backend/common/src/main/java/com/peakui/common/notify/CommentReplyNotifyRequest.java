package com.peakui.common.notify;

import lombok.Data;

/**
 * 评论回复通知（内容服务 → service-message 内部调用）。
 * 收件人由内容服务根据「内容作者 / 被回复评论作者」计算后显式传入，
 * 因此 service-message 内部接口无需依赖调用方的登录态。
 */
@Data
public class CommentReplyNotifyRequest {

    private String sourceType;
    private Long sourceId;
    private String sourceTitle;
    private Long commentId;
    private Long parentCommentId;
    private Long fromUserId;
    private String fromNickname;
    private String fromAvatar;
    private Long toUserId;
    private String contentPreview;
}
