package com.peakui.message.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 标记通知已读。ids 为空且 all=true 时全部已读。
 */
@Data
public class MarkNotificationsReadRequest {

    private List<Long> ids;
    private Boolean all;
}
