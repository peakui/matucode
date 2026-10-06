package com.peakui.message.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.message.collab.mapper.CollabDocumentMapper;
import com.peakui.message.collab.mapper.CollabOperationMapper;
import com.peakui.message.collab.model.dto.CreateCollabDocumentRequest;
import com.peakui.message.collab.model.dto.TextOperationDTO;
import com.peakui.message.collab.model.entity.CollabDocument;
import com.peakui.message.collab.model.entity.CollabOperation;
import com.peakui.message.collab.model.vo.CollabDocumentVO;
import com.peakui.message.collab.model.vo.CollabOperationVO;
import com.peakui.message.collab.model.vo.CollabRoomStateVO;
import com.peakui.message.collab.ot.OperationType;
import com.peakui.message.collab.service.CollabDocumentService;
import com.peakui.message.exception.MessageException;
import com.peakui.message.mapper.ConversationMapper;
import com.peakui.message.mapper.ConversationMemberMapper;
import com.peakui.message.model.entity.Conversation;
import com.peakui.message.model.entity.ConversationMember;
import com.peakui.message.websocket.MessageWebSocketSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollabDocumentServiceImpl implements CollabDocumentService {

    private final CollabDocumentMapper collabDocumentMapper;
    private final CollabOperationMapper collabOperationMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final MessageWebSocketSessionManager sessionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollabDocumentVO createOrGetDocument(Long conversationId, CreateCollabDocumentRequest request, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(servletRequest);
        requireConversationMember(conversationId, userId);
        CollabDocument existing = getByConversationId(conversationId);
        if (existing != null) {
            return toDocumentVO(existing);
        }
        LocalDateTime now = LocalDateTime.now();
        CollabDocument document = new CollabDocument();
        document.setConversationId(conversationId);
        document.setTitle(request == null ? null : request.getTitle());
        document.setContent(request == null || request.getContent() == null ? "" : request.getContent());
        document.setRevision(0L);
        document.setCreatedBy(userId);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        collabDocumentMapper.insert(document);
        log.info("创建协作文档成功, documentId={}, conversationId={}, userId={}",
                document.getId(), conversationId, userId);
        return toDocumentVO(document);
    }

    @Override
    public CollabDocumentVO getConversationDocument(Long conversationId, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(servletRequest);
        requireConversationMember(conversationId, userId);
        CollabDocument document = getByConversationId(conversationId);
        if (document == null) {
            throw new MessageException("协作文档不存在");
        }
        return toDocumentVO(document);
    }

    @Override
    public CollabDocumentVO getDocument(Long documentId, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(servletRequest);
        CollabDocument document = requireDocument(documentId);
        requireConversationMember(document.getConversationId(), userId);
        return toDocumentVO(document);
    }

    @Override
    public List<CollabOperationVO> listOperations(Long documentId, Long afterRevision, Integer limit, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(servletRequest);
        CollabDocument document = requireDocument(documentId);
        requireConversationMember(document.getConversationId(), userId);
        int pageSize = limit == null ? 100 : Math.min(Math.max(limit, 1), 500);
        List<CollabOperation> operations = collabOperationMapper.selectPage(new Page<>(1, pageSize), new LambdaQueryWrapper<CollabOperation>()
                .eq(CollabOperation::getDocumentId, documentId)
                .gt(CollabOperation::getRevision, afterRevision == null ? 0L : afterRevision)
                .orderByAsc(CollabOperation::getRevision)).getRecords();
        return operations.stream().map(this::toOperationVO).toList();
    }

    @Override
    public CollabRoomStateVO getRoomState(Long documentId, HttpServletRequest servletRequest) {
        Long userId = getCurrentUserId(servletRequest);
        return getRoomState(documentId, userId);
    }

    @Override
    public CollabRoomStateVO getRoomState(Long documentId, Long userId) {
        CollabDocument document = requireDocument(documentId);
        requireConversationMember(document.getConversationId(), userId);
        List<CollabOperationVO> recentOperations = collabOperationMapper.selectPage(new Page<>(1, 50), new LambdaQueryWrapper<CollabOperation>()
                        .eq(CollabOperation::getDocumentId, documentId)
                        .orderByDesc(CollabOperation::getRevision))
                .getRecords().stream().map(this::toOperationVO).toList();
        return CollabRoomStateVO.builder()
                .document(toDocumentVO(document))
                .onlineUsers(sessionManager.listRoomMembers(document.getConversationId()))
                .recentOperations(recentOperations)
                .build();
    }

    @Override
    public void requireConversationMember(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || Integer.valueOf(1).equals(conversation.getIsDeleted())) {
            throw new MessageException("会话不存在");
        }
        ConversationMember member = conversationMemberMapper.selectOne(new LambdaQueryWrapper<ConversationMember>()
                .eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                .isNull(ConversationMember::getLeftAt)
                .last("LIMIT 1"));
        if (member == null) {
            throw new MessageException("不是会话成员");
        }
    }

    private CollabDocument getByConversationId(Long conversationId) {
        return collabDocumentMapper.selectOne(new LambdaQueryWrapper<CollabDocument>()
                .eq(CollabDocument::getConversationId, conversationId)
                .last("LIMIT 1"));
    }

    private CollabDocument requireDocument(Long documentId) {
        CollabDocument document = collabDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new MessageException("协作文档不存在");
        }
        return document;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
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

    private CollabDocumentVO toDocumentVO(CollabDocument document) {
        return CollabDocumentVO.builder()
                .id(document.getId())
                .conversationId(document.getConversationId())
                .title(document.getTitle())
                .content(document.getContent())
                .revision(document.getRevision())
                .createdBy(document.getCreatedBy())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private CollabOperationVO toOperationVO(CollabOperation operation) {
        TextOperationDTO original = new TextOperationDTO();
        original.setOp(OperationType.fromCode(operation.getOperationType()).value());
        original.setPosition(operation.getPosition());
        original.setText(operation.getText());
        original.setLength(operation.getLength());
        TextOperationDTO transformed = new TextOperationDTO();
        transformed.setOp(OperationType.fromCode(operation.getOperationType()).value());
        transformed.setPosition(operation.getTransformedPosition());
        transformed.setText(operation.getText());
        transformed.setLength(operation.getLength());
        return CollabOperationVO.builder()
                .id(operation.getId())
                .documentId(operation.getDocumentId())
                .conversationId(operation.getConversationId())
                .revision(operation.getRevision())
                .baseRevision(operation.getBaseRevision())
                .userId(operation.getUserId())
                .clientId(operation.getClientId())
                .requestId(operation.getRequestId())
                .operation(original)
                .transformedOperation(transformed)
                .createdAt(operation.getCreatedAt())
                .build();
    }
}
