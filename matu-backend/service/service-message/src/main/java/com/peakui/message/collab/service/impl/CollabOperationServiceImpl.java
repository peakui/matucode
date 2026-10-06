package com.peakui.message.collab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.message.collab.mapper.CollabDocumentMapper;
import com.peakui.message.collab.mapper.CollabOperationMapper;
import com.peakui.message.collab.model.dto.CollabEditPayload;
import com.peakui.message.collab.model.dto.TextOperationDTO;
import com.peakui.message.collab.model.entity.CollabDocument;
import com.peakui.message.collab.model.entity.CollabOperation;
import com.peakui.message.collab.model.vo.CollabOperationVO;
import com.peakui.message.collab.ot.OperationType;
import com.peakui.message.collab.ot.TextDocumentApplier;
import com.peakui.message.collab.ot.TextOperation;
import com.peakui.message.collab.ot.TextOperationalTransform;
import com.peakui.message.collab.service.CollabDocumentService;
import com.peakui.message.collab.service.CollabOperationService;
import com.peakui.message.exception.MessageException;
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
public class CollabOperationServiceImpl implements CollabOperationService {

    private final CollabDocumentMapper collabDocumentMapper;
    private final CollabOperationMapper collabOperationMapper;
    private final CollabDocumentService collabDocumentService;
    private final TextOperationalTransform operationalTransform;
    private final TextDocumentApplier documentApplier;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized CollabOperationVO applyOperation(Long userId, Long conversationId, Long documentId, String clientId,
                                                        String requestId, CollabEditPayload payload) {
        if (!StringUtils.hasText(clientId)) {
            throw new MessageException("客户端ID不能为空");
        }
        if (!StringUtils.hasText(requestId)) {
            throw new MessageException("请求ID不能为空");
        }
        CollabOperation duplicated = collabOperationMapper.selectOne(new LambdaQueryWrapper<CollabOperation>()
                .eq(CollabOperation::getClientId, clientId)
                .eq(CollabOperation::getRequestId, requestId)
                .last("LIMIT 1"));
        if (duplicated != null) {
            log.debug("协同操作重复请求命中, documentId={}, userId={}, clientId={}, requestId={}",
                    documentId, userId, clientId, requestId);
            return toOperationVO(duplicated);
        }
        CollabDocument document = collabDocumentMapper.selectById(documentId);
        if (document == null || !document.getConversationId().equals(conversationId)) {
            throw new MessageException("协作文档不存在");
        }
        collabDocumentService.requireConversationMember(conversationId, userId);
        TextOperation incoming = TextOperation.fromDTO(payload.getOperation());
        Long baseRevision = payload.getBaseRevision();
        if (baseRevision > document.getRevision()) {
            throw new MessageException("客户端版本不能大于服务端版本");
        }
        TextOperation transformed = incoming.copy();
        List<CollabOperation> committedOperations = collabOperationMapper.selectList(new LambdaQueryWrapper<CollabOperation>()
                .eq(CollabOperation::getDocumentId, documentId)
                .gt(CollabOperation::getRevision, baseRevision)
                .le(CollabOperation::getRevision, document.getRevision())
                .orderByAsc(CollabOperation::getRevision));
        for (CollabOperation committed : committedOperations) {
            transformed = operationalTransform.transform(transformed, toTransformedTextOperation(committed));
        }
        String newContent = documentApplier.apply(document.getContent(), transformed);
        long nextRevision = document.getRevision() + 1;
        LocalDateTime now = LocalDateTime.now();
        CollabOperation operation = new CollabOperation();
        operation.setDocumentId(documentId);
        operation.setConversationId(conversationId);
        operation.setRevision(nextRevision);
        operation.setBaseRevision(baseRevision);
        operation.setUserId(userId);
        operation.setClientId(clientId);
        operation.setRequestId(requestId);
        operation.setOperationType(transformed.getType().code());
        operation.setPosition(incoming.getPosition());
        operation.setText(transformed.getText());
        operation.setLength(transformed.getLength());
        operation.setTransformedPosition(transformed.getPosition());
        operation.setCreatedAt(now);
        collabOperationMapper.insert(operation);
        document.setContent(newContent);
        document.setRevision(nextRevision);
        document.setUpdatedAt(now);
        collabDocumentMapper.updateById(document);
        log.debug("协同操作提交成功, documentId={}, conversationId={}, userId={}, revision={}, type={}",
                documentId, conversationId, userId, nextRevision, operation.getOperationType());
        return toOperationVO(operation);
    }

    private TextOperation toTransformedTextOperation(CollabOperation operation) {
        return new TextOperation(OperationType.fromCode(operation.getOperationType()), operation.getTransformedPosition(),
                operation.getText(), operation.getLength() == null ? 0 : operation.getLength());
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
