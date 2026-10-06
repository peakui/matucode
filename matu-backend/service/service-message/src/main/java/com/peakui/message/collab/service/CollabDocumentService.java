package com.peakui.message.collab.service;

import com.peakui.message.collab.model.dto.CreateCollabDocumentRequest;
import com.peakui.message.collab.model.vo.CollabDocumentVO;
import com.peakui.message.collab.model.vo.CollabOperationVO;
import com.peakui.message.collab.model.vo.CollabRoomStateVO;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

public interface CollabDocumentService {
    CollabDocumentVO createOrGetDocument(Long conversationId, CreateCollabDocumentRequest request, HttpServletRequest servletRequest);

    CollabDocumentVO getConversationDocument(Long conversationId, HttpServletRequest servletRequest);

    CollabDocumentVO getDocument(Long documentId, HttpServletRequest servletRequest);

    List<CollabOperationVO> listOperations(Long documentId, Long afterRevision, Integer limit, HttpServletRequest servletRequest);

    CollabRoomStateVO getRoomState(Long documentId, HttpServletRequest servletRequest);

    CollabRoomStateVO getRoomState(Long documentId, Long userId);

    void requireConversationMember(Long conversationId, Long userId);
}
