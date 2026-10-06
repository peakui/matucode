package com.peakui.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.message.exception.MessageException;
import com.peakui.message.mapper.ConversationMapper;
import com.peakui.message.mapper.ConversationMemberMapper;
import com.peakui.message.mapper.MessageMapper;
import com.peakui.message.mapper.MessageReadMapper;
import com.peakui.message.model.dto.CreateGroupConversationRequest;
import com.peakui.message.model.dto.CreateSingleConversationRequest;
import com.peakui.message.model.dto.MarkConversationReadRequest;
import com.peakui.message.model.dto.SendMessageRequest;
import com.peakui.message.model.entity.Conversation;
import com.peakui.message.model.entity.ConversationMember;
import com.peakui.message.model.entity.Message;
import com.peakui.message.model.entity.MessageRead;
import com.peakui.message.model.vo.AdminConversationVO;
import com.peakui.message.model.vo.AdminMessageVO;
import com.peakui.message.model.vo.ConversationMemberVO;
import com.peakui.message.model.vo.ConversationVO;
import com.peakui.message.model.vo.MessagePushEventVO;
import com.peakui.message.model.vo.MessageVO;
import com.peakui.message.service.MessagePushService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements com.peakui.message.service.MessageService {

    private static final int CONVERSATION_TYPE_SINGLE = 1;
    private static final int CONVERSATION_TYPE_GROUP = 2;
    private static final int MEMBER_ROLE_MEMBER = 1;
    private static final int MEMBER_ROLE_OWNER = 3;
    private static final int FLAG_NO = 0;
    private static final int FLAG_YES = 1;
    private static final int MESSAGE_TYPE_TEXT = 1;
    private static final String EVENT_MESSAGE_NEW = "MESSAGE_NEW";
    private static final String EVENT_MESSAGE_READ = "MESSAGE_READ";
    private static final String EVENT_MESSAGE_RECALL = "MESSAGE_RECALL";
    private static final String EVENT_MESSAGE_DELETE = "MESSAGE_DELETE";
    private static final String EVENT_CONVERSATION_UPDATE = "CONVERSATION_UPDATE";

    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final MessageMapper messageMapper;
    private final MessageReadMapper messageReadMapper;
    private final MessagePushService messagePushService;
    private final HttpServletRequest request;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO createSingleConversation(CreateSingleConversationRequest request) {
        Long currentUserId = getCurrentUserId();
        Long targetUserId = request.getTargetUserId();
        if (Objects.equals(currentUserId, targetUserId)) {
            throw new MessageException("不能和自己创建私信会话");
        }
        Conversation existed = findSingleConversation(currentUserId, targetUserId);
        if (existed != null) {
            restoreMember(existed.getId(), currentUserId, MEMBER_ROLE_MEMBER);
            restoreMember(existed.getId(), targetUserId, MEMBER_ROLE_MEMBER);
            log.info("复用已有单聊会话, conversationId={}, userId={}, targetUserId={}",
                    existed.getId(), currentUserId, targetUserId);
            return buildConversationVO(existed, currentUserId, true);
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setConversationType(CONVERSATION_TYPE_SINGLE);
        conversation.setCreatorId(currentUserId);
        conversation.setMemberCount(2);
        conversation.setIsDeleted(FLAG_NO);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);

        createMember(conversation.getId(), currentUserId, MEMBER_ROLE_MEMBER, now);
        createMember(conversation.getId(), targetUserId, MEMBER_ROLE_MEMBER, now);
        log.info("创建单聊会话成功, conversationId={}, userId={}, targetUserId={}",
                conversation.getId(), currentUserId, targetUserId);
        return buildConversationVO(conversation, currentUserId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationVO createGroupConversation(CreateGroupConversationRequest request) {
        Long currentUserId = getCurrentUserId();
        Set<Long> memberIds = new LinkedHashSet<>(request.getMemberIds());
        memberIds.add(currentUserId);
        if (memberIds.size() < 3) {
            throw new MessageException("群聊至少需要3个成员");
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setConversationType(CONVERSATION_TYPE_GROUP);
        conversation.setConversationName(normalizeName(request.getConversationName()));
        conversation.setCreatorId(currentUserId);
        conversation.setMemberCount(memberIds.size());
        conversation.setIsDeleted(FLAG_NO);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);

        for (Long memberId : memberIds) {
            createMember(conversation.getId(), memberId, Objects.equals(memberId, currentUserId) ? MEMBER_ROLE_OWNER : MEMBER_ROLE_MEMBER, now);
        }
        log.info("创建群聊会话成功, conversationId={}, creatorId={}, memberCount={}",
                conversation.getId(), currentUserId, memberIds.size());
        return buildConversationVO(conversation, currentUserId, true);
    }

    @Override
    public PageResponse<ConversationVO> listConversations(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        List<ConversationMember> memberships = conversationMemberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getUserId, currentUserId)
                .isNull(ConversationMember::getLeftAt)
                .orderByDesc(ConversationMember::getIsTop));
        if (memberships.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, 0, List.of());
        }
        List<Long> conversationIds = memberships.stream().map(ConversationMember::getConversationId).toList();
        Page<Conversation> page = conversationMapper.selectPage(new Page<>(currentPage, currentSize), new LambdaQueryWrapper<Conversation>()
                .in(Conversation::getId, conversationIds)
                .eq(Conversation::getIsDeleted, FLAG_NO)
                .orderByDesc(Conversation::getLastMessageTime)
                .orderByDesc(Conversation::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords().stream()
                .map(conversation -> buildConversationVO(conversation, currentUserId, false))
                .toList());
    }

    @Override
    public ConversationVO getConversation(Long conversationId) {
        Long currentUserId = getCurrentUserId();
        requireMember(conversationId, currentUserId);
        return buildConversationVO(getConversationEntity(conversationId), currentUserId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long conversationId, SendMessageRequest request) {
        Long currentUserId = getCurrentUserId();
        requireMember(conversationId, currentUserId);
        validateMessage(request);
        if (request.getReplyToId() != null) {
            Message reply = messageMapper.selectById(request.getReplyToId());
            if (reply == null || !Objects.equals(reply.getConversationId(), conversationId)) {
                throw new MessageException("回复消息不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setMessageType(request.getMessageType() == null ? MESSAGE_TYPE_TEXT : request.getMessageType());
        message.setContent(request.getContent());
        message.setFileUrl(request.getFileUrl());
        message.setFileName(request.getFileName());
        message.setFileSize(request.getFileSize() == null ? 0L : request.getFileSize());
        message.setReplyToId(request.getReplyToId());
        message.setIsDeleted(FLAG_NO);
        message.setIsRecall(FLAG_NO);
        message.setReadStatus(FLAG_NO);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messageMapper.insert(message);

        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, conversationId)
                .set(Conversation::getLastMessageId, message.getId())
                .set(Conversation::getLastMessageTime, now)
                .set(Conversation::getUpdatedAt, now));
        conversationMemberMapper.update(null, new LambdaUpdateWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .ne(ConversationMember::getUserId, currentUserId)
                .isNull(ConversationMember::getLeftAt)
                .setSql("unread_count = IFNULL(unread_count, 0) + 1"));
        markRead(message.getId(), currentUserId, now);
        MessageVO messageVO = toMessageVO(message);
        List<Long> memberIds = listActiveMemberIds(conversationId);
        pushEvent(EVENT_MESSAGE_NEW, conversationId, message.getId(), currentUserId, memberIds, messageVO);
        pushEvent(EVENT_CONVERSATION_UPDATE, conversationId, message.getId(), currentUserId, memberIds, buildConversationVO(getConversationEntity(conversationId), currentUserId, false));
        log.info("发送消息成功, conversationId={}, messageId={}, senderId={}, type={}, receiverCount={}",
                conversationId, message.getId(), currentUserId, message.getMessageType(), memberIds.size());
        return messageVO;
    }

    @Override
    public PageResponse<MessageVO> listMessages(Long conversationId, Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        requireMember(conversationId, currentUserId);
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<Message> page = messageMapper.selectPage(new Page<>(currentPage, currentSize), new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getIsDeleted, FLAG_NO)
                .orderByDesc(Message::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords().stream().map(this::toMessageVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Long conversationId, MarkConversationReadRequest request) {
        Long currentUserId = getCurrentUserId();
        ConversationMember member = requireMember(conversationId, currentUserId);
        Long lastReadMessageId = request.getLastReadMessageId();
        if (lastReadMessageId == null) {
            Message lastMessage = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, conversationId)
                    .eq(Message::getIsDeleted, FLAG_NO)
                    .orderByDesc(Message::getId)
                    .last("limit 1"));
            lastReadMessageId = lastMessage == null ? null : lastMessage.getId();
        }
        if (lastReadMessageId == null) {
            clearUnread(member.getId(), null);
            log.info("会话无消息，清空未读, conversationId={}, userId={}", conversationId, currentUserId);
            return;
        }
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .le(Message::getId, lastReadMessageId)
                .ne(Message::getSenderId, currentUserId)
                .eq(Message::getIsDeleted, FLAG_NO));
        LocalDateTime now = LocalDateTime.now();
        for (Message message : messages) {
            markRead(message.getId(), currentUserId, now);
        }
        clearUnread(member.getId(), lastReadMessageId);
        pushEvent(EVENT_MESSAGE_READ, conversationId, lastReadMessageId, currentUserId, listActiveMemberIds(conversationId), Map.of(
                "userId", currentUserId,
                "lastReadMessageId", lastReadMessageId
        ));
        log.info("标记会话已读成功, conversationId={}, userId={}, lastReadMessageId={}, readCount={}",
                conversationId, currentUserId, lastReadMessageId, messages.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId) {
        Long currentUserId = getCurrentUserId();
        Message message = getOwnedMessage(messageId, currentUserId);
        if (Objects.equals(message.getIsDeleted(), FLAG_YES)) {
            return;
        }
        message.setIsDeleted(FLAG_YES);
        message.setUpdatedAt(LocalDateTime.now());
        messageMapper.updateById(message);
        pushEvent(EVENT_MESSAGE_DELETE, message.getConversationId(), message.getId(), currentUserId, listActiveMemberIds(message.getConversationId()), toMessageVO(message));
        log.info("删除消息成功, messageId={}, conversationId={}, userId={}",
                messageId, message.getConversationId(), currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallMessage(Long messageId) {
        Long currentUserId = getCurrentUserId();
        Message message = getOwnedMessage(messageId, currentUserId);
        if (Objects.equals(message.getIsRecall(), FLAG_YES)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        message.setIsRecall(FLAG_YES);
        message.setRecallTime(now);
        message.setUpdatedAt(now);
        messageMapper.updateById(message);
        pushEvent(EVENT_MESSAGE_RECALL, message.getConversationId(), message.getId(), currentUserId, listActiveMemberIds(message.getConversationId()), toMessageVO(message));
        log.info("撤回消息成功, messageId={}, conversationId={}, userId={}",
                messageId, message.getConversationId(), currentUserId);
    }

    @Override
    public void topConversation(Long conversationId) {
        updateMemberFlag(conversationId, getCurrentUserId(), true, FLAG_YES);
    }

    @Override
    public void untopConversation(Long conversationId) {
        updateMemberFlag(conversationId, getCurrentUserId(), true, FLAG_NO);
    }

    @Override
    public void muteConversation(Long conversationId) {
        updateMemberFlag(conversationId, getCurrentUserId(), false, FLAG_YES);
    }

    @Override
    public void unmuteConversation(Long conversationId) {
        updateMemberFlag(conversationId, getCurrentUserId(), false, FLAG_NO);
    }

    @Override
    public PageResponse<AdminConversationVO> adminListConversations(String keyword, Integer conversationType,
                                                                    Long creatorId, Long pageNum, Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<Conversation> page = conversationMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<Conversation>()
                        .eq(conversationType != null, Conversation::getConversationType, conversationType)
                        .eq(creatorId != null, Conversation::getCreatorId, creatorId)
                        .like(StringUtils.hasText(keyword), Conversation::getConversationName, keyword)
                        .orderByDesc(Conversation::getLastMessageTime)
                        .orderByDesc(Conversation::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(conversation -> toAdminConversationVO(conversation, false)).toList());
    }

    @Override
    public AdminConversationVO adminGetConversation(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new MessageException("会话不存在");
        }
        return toAdminConversationVO(conversation, true);
    }

    @Override
    public PageResponse<AdminMessageVO> adminListMessages(Long conversationId, Long senderId, Integer messageType,
                                                          Integer isRecall, Integer isDeleted, String keyword,
                                                          LocalDateTime startTime, LocalDateTime endTime,
                                                          Long pageNum, Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        Page<Message> page = messageMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<Message>()
                        .eq(conversationId != null, Message::getConversationId, conversationId)
                        .eq(senderId != null, Message::getSenderId, senderId)
                        .eq(messageType != null, Message::getMessageType, messageType)
                        .eq(isRecall != null, Message::getIsRecall, isRecall)
                        .eq(isDeleted != null, Message::getIsDeleted, isDeleted)
                        .like(StringUtils.hasText(keyword), Message::getContent, keyword)
                        .ge(startTime != null, Message::getCreatedAt, startTime)
                        .le(endTime != null, Message::getCreatedAt, endTime)
                        .orderByDesc(Message::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::toAdminMessageVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRecallMessage(Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new MessageException("消息不存在");
        }
        if (Objects.equals(message.getIsRecall(), FLAG_YES)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        message.setIsRecall(FLAG_YES);
        message.setRecallTime(now);
        message.setUpdatedAt(now);
        messageMapper.updateById(message);
        pushEvent(EVENT_MESSAGE_RECALL, message.getConversationId(), message.getId(), message.getSenderId(),
                listActiveMemberIds(message.getConversationId()), toMessageVO(message));
        log.info("管理员撤回消息成功, messageId={}, conversationId={}, senderId={}",
                messageId, message.getConversationId(), message.getSenderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteMessage(Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new MessageException("消息不存在");
        }
        if (Objects.equals(message.getIsDeleted(), FLAG_YES)) {
            return;
        }
        message.setIsDeleted(FLAG_YES);
        message.setUpdatedAt(LocalDateTime.now());
        messageMapper.updateById(message);
        pushEvent(EVENT_MESSAGE_DELETE, message.getConversationId(), message.getId(), message.getSenderId(),
                listActiveMemberIds(message.getConversationId()), toMessageVO(message));
        log.info("管理员删除消息成功, messageId={}, conversationId={}, senderId={}",
                messageId, message.getConversationId(), message.getSenderId());
    }

    private AdminConversationVO toAdminConversationVO(Conversation conversation, boolean withMembers) {
        Message lastMessage = conversation.getLastMessageId() == null ? null : messageMapper.selectById(conversation.getLastMessageId());
        List<ConversationMemberVO> members = withMembers
                ? conversationMemberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConversationId, conversation.getId())
                        .orderByAsc(ConversationMember::getJoinedAt))
                .stream()
                .map(this::toMemberVO)
                .toList()
                : null;
        return AdminConversationVO.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .conversationName(conversation.getConversationName())
                .creatorId(conversation.getCreatorId())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageTime(conversation.getLastMessageTime())
                .lastMessagePreview(buildMessagePreview(lastMessage))
                .memberCount(conversation.getMemberCount())
                .isDeleted(conversation.getIsDeleted())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .members(members)
                .build();
    }

    private AdminMessageVO toAdminMessageVO(Message message) {
        return AdminMessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .replyToId(message.getReplyToId())
                .isDeleted(message.getIsDeleted())
                .isRecall(message.getIsRecall())
                .recallTime(message.getRecallTime())
                .readStatus(message.getReadStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String buildMessagePreview(Message message) {
        if (message == null) {
            return null;
        }
        if (Objects.equals(message.getIsRecall(), FLAG_YES)) {
            return "[已撤回]";
        }
        String content = message.getContent();
        if (StringUtils.hasText(content)) {
            return content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }
        return StringUtils.hasText(message.getFileName()) ? "[文件] " + message.getFileName() : "[消息]";
    }

    private Conversation findSingleConversation(Long userId, Long targetUserId) {
        List<ConversationMember> userMemberships = conversationMemberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getUserId, userId));
        if (userMemberships.isEmpty()) {
            return null;
        }
        List<Long> conversationIds = userMemberships.stream().map(ConversationMember::getConversationId).toList();
        List<Conversation> conversations = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .in(Conversation::getId, conversationIds)
                .eq(Conversation::getConversationType, CONVERSATION_TYPE_SINGLE)
                .eq(Conversation::getIsDeleted, FLAG_NO));
        for (Conversation conversation : conversations) {
            long count = conversationMemberMapper.selectCount(new LambdaQueryWrapper<ConversationMember>()
                    .eq(ConversationMember::getConversationId, conversation.getId())
                    .in(ConversationMember::getUserId, List.of(userId, targetUserId)));
            if (count == 2) {
                return conversation;
            }
        }
        return null;
    }

    private void restoreMember(Long conversationId, Long userId, Integer role) {
        ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                .last("limit 1"));
        if (member == null) {
            createMember(conversationId, userId, role, LocalDateTime.now());
            return;
        }
        if (member.getLeftAt() != null) {
            member.setLeftAt(null);
            member.setJoinedAt(LocalDateTime.now());
            conversationMemberMapper.updateById(member);
        }
    }

    private void createMember(Long conversationId, Long userId, Integer role, LocalDateTime now) {
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setRole(role);
        member.setIsMuted(FLAG_NO);
        member.setIsTop(FLAG_NO);
        member.setUnreadCount(0);
        member.setJoinedAt(now);
        conversationMemberMapper.insert(member);
    }

    private ConversationMember requireMember(Long conversationId, Long userId) {
        Conversation conversation = getConversationEntity(conversationId);
        ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversation.getId())
                .eq(ConversationMember::getUserId, userId)
                .isNull(ConversationMember::getLeftAt)
                .last("limit 1"));
        if (member == null) {
            throw new MessageException("无权访问该会话");
        }
        return member;
    }

    private Conversation getConversationEntity(Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || Objects.equals(conversation.getIsDeleted(), FLAG_YES)) {
            throw new MessageException("会话不存在");
        }
        return conversation;
    }

    private Message getOwnedMessage(Long messageId, Long currentUserId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null || Objects.equals(message.getIsDeleted(), FLAG_YES)) {
            throw new MessageException("消息不存在");
        }
        requireMember(message.getConversationId(), currentUserId);
        if (!Objects.equals(message.getSenderId(), currentUserId)) {
            throw new MessageException("只能操作自己发送的消息");
        }
        return message;
    }

    private void updateMemberFlag(Long conversationId, Long currentUserId, boolean top, Integer value) {
        ConversationMember member = requireMember(conversationId, currentUserId);
        if (top) {
            member.setIsTop(value);
        } else {
            member.setIsMuted(value);
        }
        conversationMemberMapper.updateById(member);
        log.info("更新会话成员标记成功, conversationId={}, userId={}, flag={}, value={}",
                conversationId, currentUserId, top ? "top" : "muted", value);
    }

    private void validateMessage(SendMessageRequest request) {
        Integer type = request.getMessageType() == null ? MESSAGE_TYPE_TEXT : request.getMessageType();
        if (type == MESSAGE_TYPE_TEXT && !StringUtils.hasText(request.getContent())) {
            throw new MessageException("文本消息内容不能为空");
        }
        if (type != MESSAGE_TYPE_TEXT && !StringUtils.hasText(request.getFileUrl())) {
            throw new MessageException("文件消息地址不能为空");
        }
    }

    private void markRead(Long messageId, Long userId, LocalDateTime now) {
        long count = messageReadMapper.selectCount(new LambdaQueryWrapper<MessageRead>()
                .eq(MessageRead::getMessageId, messageId)
                .eq(MessageRead::getUserId, userId));
        if (count > 0) {
            return;
        }
        MessageRead read = new MessageRead();
        read.setMessageId(messageId);
        read.setUserId(userId);
        read.setReadAt(now);
        messageReadMapper.insert(read);
    }

    private void clearUnread(Long memberId, Long lastReadMessageId) {
        LambdaUpdateWrapper<ConversationMember> wrapper = new LambdaUpdateWrapper<ConversationMember>()
                .eq(ConversationMember::getId, memberId)
                .set(ConversationMember::getUnreadCount, 0);
        if (lastReadMessageId != null) {
            wrapper.set(ConversationMember::getLastReadMessageId, lastReadMessageId);
        }
        conversationMemberMapper.update(null, wrapper);
    }

    private List<Long> listActiveMemberIds(Long conversationId) {
        return conversationMemberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConversationId, conversationId)
                        .isNull(ConversationMember::getLeftAt))
                .stream()
                .map(ConversationMember::getUserId)
                .toList();
    }

    private void pushEvent(String type, Long conversationId, Long messageId, Long senderId, List<Long> receiverUserIds, Object data) {
        messagePushService.pushToUsers(receiverUserIds, MessagePushEventVO.builder()
                .type(type)
                .conversationId(conversationId)
                .messageId(messageId)
                .senderId(senderId)
                .receiverUserIds(receiverUserIds)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private ConversationVO buildConversationVO(Conversation conversation, Long currentUserId, boolean withMembers) {
        ConversationMember self = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversation.getId())
                .eq(ConversationMember::getUserId, currentUserId)
                .last("limit 1"));
        Message lastMessage = conversation.getLastMessageId() == null ? null : messageMapper.selectById(conversation.getLastMessageId());
        List<ConversationMemberVO> members = withMembers
                ? conversationMemberMapper.selectList(new LambdaQueryWrapper<ConversationMember>()
                        .eq(ConversationMember::getConversationId, conversation.getId())
                        .isNull(ConversationMember::getLeftAt))
                .stream()
                .map(this::toMemberVO)
                .toList()
                : null;
        return ConversationVO.builder()
                .id(conversation.getId())
                .conversationType(conversation.getConversationType())
                .conversationName(conversation.getConversationName())
                .creatorId(conversation.getCreatorId())
                .lastMessageId(conversation.getLastMessageId())
                .lastMessageTime(conversation.getLastMessageTime())
                .memberCount(conversation.getMemberCount())
                .isMuted(self == null ? 0 : self.getIsMuted())
                .isTop(self == null ? 0 : self.getIsTop())
                .lastReadMessageId(self == null ? null : self.getLastReadMessageId())
                .unreadCount(self == null ? 0 : self.getUnreadCount())
                .lastMessage(lastMessage == null ? null : toMessageVO(lastMessage))
                .members(members)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private ConversationMemberVO toMemberVO(ConversationMember member) {
        return ConversationMemberVO.builder()
                .id(member.getId())
                .conversationId(member.getConversationId())
                .userId(member.getUserId())
                .role(member.getRole())
                .isMuted(member.getIsMuted())
                .isTop(member.getIsTop())
                .lastReadMessageId(member.getLastReadMessageId())
                .unreadCount(member.getUnreadCount())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private MessageVO toMessageVO(Message message) {
        return MessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .replyToId(message.getReplyToId())
                .isDeleted(message.getIsDeleted())
                .isRecall(message.getIsRecall())
                .recallTime(message.getRecallTime())
                .readStatus(message.getReadStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String normalizeName(String name) {
        return StringUtils.hasText(name) ? name.trim() : "群聊";
    }

    private Long getCurrentUserId() {
        String userId = request.getHeader("X-User-Id");
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
