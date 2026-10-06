package com.peakui.info.service;

import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateAnnouncementRequest;
import com.peakui.info.model.dto.UpdateAnnouncementRequest;
import com.peakui.info.model.vo.AnnouncementDetailVO;
import com.peakui.info.model.vo.AnnouncementListItemVO;

public interface AnnouncementService {

    PageResponse<AnnouncementListItemVO> listPublicAnnouncements(Integer type, String keyword, Long pageNum, Long pageSize);

    AnnouncementDetailVO getPublicAnnouncementDetail(Long id);

    PageResponse<AnnouncementListItemVO> listAdminAnnouncements(Integer type, Integer status, String keyword, Long pageNum, Long pageSize);

    AnnouncementDetailVO getAdminAnnouncementDetail(Long id);

    AnnouncementDetailVO createAnnouncement(CreateAnnouncementRequest request);

    AnnouncementDetailVO updateAnnouncement(Long id, UpdateAnnouncementRequest request);

    void offlineAnnouncement(Long id);

    void publishAnnouncement(Long id);
}
