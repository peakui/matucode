package com.peakui.message.service;

import com.peakui.message.model.vo.MessagePushEventVO;

import java.util.Collection;

public interface MessagePushService {

    void pushToUsers(Collection<Long> userIds, MessagePushEventVO event);
}
