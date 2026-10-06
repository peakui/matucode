package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateAnnouncementRequest;
import com.peakui.info.model.dto.UpdateAnnouncementRequest;
import com.peakui.info.model.vo.AnnouncementDetailVO;
import com.peakui.info.model.vo.AnnouncementListItemVO;
import com.peakui.info.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公告管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/info/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "管理端公告列表")
    @GetMapping
    public ApiResponse<PageResponse<AnnouncementListItemVO>> listAnnouncements(@RequestParam(required = false) Integer type,
                                                                               @RequestParam(required = false) Integer status,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestParam(defaultValue = "1") Long pageNum,
                                                                               @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(announcementService.listAdminAnnouncements(type, status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "管理端公告详情")
    @GetMapping("/{id}")
    public ApiResponse<AnnouncementDetailVO> getAnnouncementDetail(@PathVariable Long id) {
        return ApiResponse.success(announcementService.getAdminAnnouncementDetail(id));
    }

    @Operation(summary = "创建公告")
    @PostMapping
    public ApiResponse<AnnouncementDetailVO> createAnnouncement(@Valid @RequestBody CreateAnnouncementRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), announcementService.createAnnouncement(request));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    public ApiResponse<AnnouncementDetailVO> updateAnnouncement(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateAnnouncementRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), announcementService.updateAnnouncement(id, request));
    }

    @Operation(summary = "下架公告")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.offlineAnnouncement(id);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishAnnouncement(@PathVariable Long id) {
        announcementService.publishAnnouncement(id);
        return ApiResponse.success(CommonError.PUBLISH_SUCCESS.message(), null);
    }

    @Operation(summary = "下架公告")
    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offlineAnnouncement(@PathVariable Long id) {
        announcementService.offlineAnnouncement(id);
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), null);
    }
}
