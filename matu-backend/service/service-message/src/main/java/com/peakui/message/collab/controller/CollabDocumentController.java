package com.peakui.message.collab.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.message.collab.model.dto.CreateCollabDocumentRequest;
import com.peakui.message.collab.model.vo.CollabDocumentVO;
import com.peakui.message.collab.model.vo.CollabOperationVO;
import com.peakui.message.collab.model.vo.CollabRoomStateVO;
import com.peakui.message.collab.service.CollabDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "协同编辑接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/messages/collab")
public class CollabDocumentController {

    private final CollabDocumentService collabDocumentService;

    @Operation(summary = "创建或获取会话协作文档")
    @PostMapping("/conversations/{conversationId}/document")
    public ApiResponse<CollabDocumentVO> createOrGetDocument(@PathVariable Long conversationId,
                                                             @Valid @RequestBody(required = false) CreateCollabDocumentRequest request,
                                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(collabDocumentService.createOrGetDocument(conversationId, request, servletRequest));
    }

    @Operation(summary = "获取会话协作文档")
    @GetMapping("/conversations/{conversationId}/document")
    public ApiResponse<CollabDocumentVO> getConversationDocument(@PathVariable Long conversationId,
                                                                 HttpServletRequest servletRequest) {
        return ApiResponse.success(collabDocumentService.getConversationDocument(conversationId, servletRequest));
    }

    @Operation(summary = "获取协作文档详情")
    @GetMapping("/documents/{documentId}")
    public ApiResponse<CollabDocumentVO> getDocument(@PathVariable Long documentId, HttpServletRequest servletRequest) {
        return ApiResponse.success(collabDocumentService.getDocument(documentId, servletRequest));
    }

    @Operation(summary = "获取协作文档操作历史")
    @GetMapping("/documents/{documentId}/operations")
    public ApiResponse<List<CollabOperationVO>> listOperations(@PathVariable Long documentId,
                                                               @RequestParam(defaultValue = "0") Long afterRevision,
                                                               @RequestParam(defaultValue = "100") Integer limit,
                                                               HttpServletRequest servletRequest) {
        return ApiResponse.success(collabDocumentService.listOperations(documentId, afterRevision, limit, servletRequest));
    }

    @Operation(summary = "获取协作房间状态")
    @GetMapping("/documents/{documentId}/room-state")
    public ApiResponse<CollabRoomStateVO> getRoomState(@PathVariable Long documentId, HttpServletRequest servletRequest) {
        return ApiResponse.success(collabDocumentService.getRoomState(documentId, servletRequest));
    }
}
