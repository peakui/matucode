package com.peakui.info.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.vo.AnnouncementDetailVO;
import com.peakui.info.model.vo.AnnouncementListItemVO;
import com.peakui.info.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "公告接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表")
    @GetMapping
    public ApiResponse<PageResponse<AnnouncementListItemVO>> listAnnouncements(@RequestParam(required = false) Integer type,
                                                                               @RequestParam(required = false) String keyword,
                                                                               @RequestParam(defaultValue = "1") Long pageNum,
                                                                               @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(announcementService.listPublicAnnouncements(type, keyword, pageNum, pageSize));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public ApiResponse<AnnouncementDetailVO> getAnnouncementDetail(@PathVariable Long id) {
        return ApiResponse.success(announcementService.getPublicAnnouncementDetail(id));
    }
}
