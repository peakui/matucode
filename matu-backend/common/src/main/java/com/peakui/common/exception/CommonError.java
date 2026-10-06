package com.peakui.common.exception;

public enum CommonError {

    SUCCESS(0, "success"),
    PARAM_VALIDATION_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    INTERNAL_SERVER_ERROR(500, "系统异常，请稍后再试"),
    CREATE_SUCCESS(0, "创建成功"),
    UPDATE_SUCCESS(0, "更新成功"),
    DELETE_SUCCESS(0, "删除成功"),
    UPLOAD_SUCCESS(0, "上传成功"),
    PUBLISH_SUCCESS(0, "发布成功"),
    SUBMIT_SUCCESS(0, "提交成功"),
    REVIEW_SUCCESS(0, "审核成功"),
    LOGOUT_SUCCESS(0, "退出成功"),
    FOLLOW_SUCCESS(0, "关注成功"),
    UNFOLLOW_SUCCESS(0, "取消关注成功"),
    COLLECT_SUCCESS(0, "收藏成功"),
    UNCOLLECT_SUCCESS(0, "取消收藏成功"),
    LIKE_SUCCESS(0, "点赞成功"),
    UNLIKE_SUCCESS(0, "取消点赞成功"),
    COMMENT_SUCCESS(0, "评论成功"),
    VOTE_SUCCESS(0, "投票成功");

    private final Integer code;
    private final String message;

    CommonError(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return code;
    }

    public String message() {
        return message;
    }
}
