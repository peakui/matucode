package com.peakui.message.service;

import com.peakui.common.result.PageResponse;
import com.peakui.message.model.dto.CreateGroupConversationRequest;
import com.peakui.message.model.dto.CreateSingleConversationRequest;
import com.peakui.message.model.dto.MarkConversationReadRequest;
import com.peakui.message.model.dto.SendMessageRequest;
import com.peakui.message.model.vo.AdminConversationVO;
import com.peakui.message.model.vo.AdminMessageVO;
import com.peakui.message.model.vo.ConversationVO;
import com.peakui.message.model.vo.MessageVO;

import java.time.LocalDateTime;

public interface MessageService {
    ConversationVO createSingleConversation(CreateSingleConversationRequest request);

    ConversationVO createGroupConversation(CreateGroupConversationRequest request);

    PageResponse<ConversationVO> listConversations(Long pageNum, Long pageSize);

    ConversationVO getConversation(Long conversationId);

    MessageVO sendMessage(Long conversationId, SendMessageRequest request);

    PageResponse<MessageVO> listMessages(Long conversationId, Long pageNum, Long pageSize);

    void markConversationRead(Long conversationId, MarkConversationReadRequest request);

    void deleteMessage(Long messageId);

    void recallMessage(Long messageId);

    void topConversation(Long conversationId);

    void untopConversation(Long conversationId);

    void muteConversation(Long conversationId);

    void unmuteConversation(Long conversationId);

    PageResponse<AdminConversationVO> adminListConversations(String keyword, Integer conversationType, Long creatorId,
                                                             Long pageNum, Long pageSize);

    AdminConversationVO adminGetConversation(Long conversationId);

    PageResponse<AdminMessageVO> adminListMessages(Long conversationId, Long senderId, Integer messageType,
                                                   Integer isRecall, Integer isDeleted, String keyword,
                                                   LocalDateTime startTime, LocalDateTime endTime,
                                                   Long pageNum, Long pageSize);

    void adminRecallMessage(Long messageId);

    void adminDeleteMessage(Long messageId);
}
