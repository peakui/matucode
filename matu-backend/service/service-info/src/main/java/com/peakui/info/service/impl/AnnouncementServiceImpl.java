package com.peakui.info.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.info.exception.InfoException;
import com.peakui.info.mapper.AnnouncementMapper;
import com.peakui.info.model.dto.CreateAnnouncementRequest;
import com.peakui.info.model.dto.UpdateAnnouncementRequest;
import com.peakui.info.model.entity.Announcement;
import com.peakui.info.model.vo.AnnouncementDetailVO;
import com.peakui.info.model.vo.AnnouncementListItemVO;
import com.peakui.info.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public PageResponse<AnnouncementListItemVO> listPublicAnnouncements(Integer type, String keyword, Long pageNum, Long pageSize) {
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .and(w -> w.isNull(Announcement::getPublishTime).or().le(Announcement::getPublishTime, now))
                .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now));
        applyAnnouncementFilters(wrapper, type, null, keyword);
        applyAnnouncementSort(wrapper);

        Page<Announcement> page = announcementMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toListItemVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDetailVO getPublicAnnouncementDetail(Long id) {
        Announcement announcement = getCurrentVisibleAnnouncement(id);
        announcementMapper.update(null, new LambdaUpdateWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .setSql("click_count = IFNULL(click_count, 0) + 1"));
        announcement.setClickCount((announcement.getClickCount() == null ? 0 : announcement.getClickCount()) + 1);
        return toDetailVO(announcement);
    }

    @Override
    public PageResponse<AnnouncementListItemVO> listAdminAnnouncements(Integer type, Integer status, String keyword, Long pageNum, Long pageSize) {
        checkAdmin();
        long currentPage = normalizePageNum(pageNum);
        long currentSize = normalizePageSize(pageSize);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        applyAnnouncementFilters(wrapper, type, status, keyword);
        applyAnnouncementSort(wrapper);

        Page<Announcement> page = announcementMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        return PageResponse.of(currentPage, currentSize, page.getTotal(), page.getRecords().stream().map(this::toListItemVO).toList());
    }

    @Override
    public AnnouncementDetailVO getAdminAnnouncementDetail(Long id) {
        checkAdmin();
        return toDetailVO(getAnnouncement(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDetailVO createAnnouncement(CreateAnnouncementRequest request) {
        checkAdmin();
        validateAnnouncementTimes(request.getPublishTime(), request.getExpireTime());
        LocalDateTime now = LocalDateTime.now();
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent());
        announcement.setType(request.getType() == null ? 0 : request.getType());
        announcement.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        announcement.setIsPinned(request.getIsPinned() == null ? 0 : request.getIsPinned());
        announcement.setPublishTime(request.getPublishTime());
        announcement.setExpireTime(request.getExpireTime());
        announcement.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        announcement.setAuthorId(StpUtil.getLoginIdAsLong());
        announcement.setClickCount(0);
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);
        announcementMapper.insert(announcement);
        log.info("创建公告成功, announcementId={}, title={}, status={}",
                announcement.getId(), announcement.getTitle(), announcement.getStatus());
        return toDetailVO(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementDetailVO updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        checkAdmin();
        validateAnnouncementTimes(request.getPublishTime(), request.getExpireTime());
        Announcement announcement = getAnnouncement(id);
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent());
        announcement.setType(request.getType() == null ? 0 : request.getType());
        announcement.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        announcement.setIsPinned(request.getIsPinned() == null ? 0 : request.getIsPinned());
        announcement.setPublishTime(request.getPublishTime());
        announcement.setExpireTime(request.getExpireTime());
        announcement.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        announcement.setAuthorId(StpUtil.getLoginIdAsLong());
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        log.info("更新公告成功, announcementId={}, title={}, status={}",
                announcement.getId(), announcement.getTitle(), announcement.getStatus());
        return toDetailVO(announcement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineAnnouncement(Long id) {
        checkAdmin();
        Announcement announcement = getAnnouncement(id);
        announcement.setStatus(2);
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        log.info("下架公告成功, announcementId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(Long id) {
        checkAdmin();
        Announcement announcement = getAnnouncement(id);
        validateAnnouncementTimes(announcement.getPublishTime(), announcement.getExpireTime());
        announcement.setStatus(1);
        announcement.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        log.info("发布公告成功, announcementId={}", id);
    }

    private Announcement getCurrentVisibleAnnouncement(Long id) {
        LocalDateTime now = LocalDateTime.now();
        Announcement announcement = announcementMapper.selectOne(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .eq(Announcement::getStatus, 1)
                .and(w -> w.isNull(Announcement::getPublishTime).or().le(Announcement::getPublishTime, now))
                .and(w -> w.isNull(Announcement::getExpireTime).or().ge(Announcement::getExpireTime, now)));
        if (announcement == null) {
            throw new InfoException("公告不存在或未发布");
        }
        return announcement;
    }

    private Announcement getAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new InfoException("公告不存在");
        }
        return announcement;
    }

    private void applyAnnouncementFilters(LambdaQueryWrapper<Announcement> wrapper, Integer type, Integer status, String keyword) {
        if (type != null) {
            wrapper.eq(Announcement::getType, type);
        }
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Announcement::getTitle, keyword).or().like(Announcement::getContent, keyword));
        }
    }

    private void applyAnnouncementSort(LambdaQueryWrapper<Announcement> wrapper) {
        wrapper.orderByDesc(Announcement::getIsPinned, Announcement::getPriority, Announcement::getPublishTime, Announcement::getCreatedAt);
    }

    private void validateAnnouncementTimes(LocalDateTime publishTime, LocalDateTime expireTime) {
        if (expireTime != null) {
            LocalDateTime start = publishTime == null ? LocalDateTime.now() : publishTime;
            if (!expireTime.isAfter(start)) {
                throw new InfoException("公告过期时间必须晚于发布时间");
            }
        }
    }

    private AnnouncementListItemVO toListItemVO(Announcement announcement) {
        return AnnouncementListItemVO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .isPinned(announcement.getIsPinned())
                .publishTime(announcement.getPublishTime())
                .expireTime(announcement.getExpireTime())
                .status(announcement.getStatus())
                .authorId(announcement.getAuthorId())
                .clickCount(announcement.getClickCount())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }

    private AnnouncementDetailVO toDetailVO(Announcement announcement) {
        return AnnouncementDetailVO.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .type(announcement.getType())
                .priority(announcement.getPriority())
                .isPinned(announcement.getIsPinned())
                .publishTime(announcement.getPublishTime())
                .expireTime(announcement.getExpireTime())
                .status(announcement.getStatus())
                .authorId(announcement.getAuthorId())
                .clickCount(announcement.getClickCount())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }

    private void checkAdmin() {
        StpUtil.checkLogin();
        StpUtil.checkRole("ADMIN");
    }
}
