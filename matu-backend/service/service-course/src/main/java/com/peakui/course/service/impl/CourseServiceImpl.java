package com.peakui.course.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.course.exception.CourseException;
import com.peakui.course.feign.FileFeignClient;
import com.peakui.course.feign.UserFeignClient;
import com.peakui.course.feign.vo.UserProfileVO;
import com.peakui.course.service.VideoMetadataService;
import com.peakui.course.mapper.*;
import com.peakui.course.model.dto.*;
import com.peakui.course.model.entity.*;
import com.peakui.course.model.vo.*;
import com.peakui.course.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String HEADER_USER_ROLES = "X-User-Roles";

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseVideoMapper videoMapper;
    private final CourseProgressMapper progressMapper;
    private final CourseNoteMapper noteMapper;
    private final CourseReviewMapper reviewMapper;
    private final CourseCertificateMapper certificateMapper;
    private final CourseArticleMapper articleMapper;
    private final FileFeignClient fileFeignClient;
    private final UserFeignClient userFeignClient;
    private final VideoMetadataService videoMetadataService;
    private final HttpServletRequest request;
    private final com.peakui.course.feign.CourseEntitlementClient entitlementClient;

    public PageResponse<CourseArticleVO> listPublishedArticles(String keyword, Long pageNum, Long pageSize) {
        long page = pageNum == null ? 1 : Math.max(1,pageNum), size = pageSize == null ? 12 : Math.max(1,Math.min(50,pageSize));
        var result = articleMapper.selectPage(new Page<CourseArticle>(page,size), new LambdaQueryWrapper<CourseArticle>()
                .select(CourseArticle::getId,CourseArticle::getCourseId,CourseArticle::getChapterId,CourseArticle::getTitle,
                        CourseArticle::getWordCount,CourseArticle::getReadTime,CourseArticle::getViewCount,CourseArticle::getSortOrder)
                .eq(CourseArticle::getStatus,1).inSql(CourseArticle::getCourseId,"SELECT id FROM courses WHERE status=1")
                .like(StringUtils.hasText(keyword),CourseArticle::getTitle,keyword).orderByDesc(CourseArticle::getUpdatedAt));
        return PageResponse.of(page,size,result.getTotal(),result.getRecords().stream().map(article -> {
            var vo=articleVO(article);vo.setContent(null);return vo;
        }).toList());
    }

    public PageResponse<CourseListItemVO> listCourses(Integer status,
                                                      Long categoryId,
                                                      Integer level,
                                                      Boolean freeOnly,
                                                      String keyword,
                                                      Long pageNum,
                                                      Long pageSize,
                                                      String sortBy,
                                                      Boolean mine) {
        boolean isManager = manager();
        // 非管理员查看“我的课程”：按发布者过滤并放开状态，草稿也能看到，否则永远无法发布自己的草稿
        boolean mineOnly = Boolean.TRUE.equals(mine) && !isManager;
        Long mineUserId = mineOnly ? uid() : null;
        boolean filterByStatus;
        Integer statusFilter;
        if (mineOnly) {
            filterByStatus = status != null;
            statusFilter = status;
        } else {
            filterByStatus = !isManager || status != null;
            statusFilter = isManager ? status : Integer.valueOf(1);
        }
        LambdaQueryWrapper<Course> q = new LambdaQueryWrapper<Course>()
                .eq(mineOnly, Course::getInstructorId, mineUserId)
                .eq(filterByStatus, Course::getStatus, statusFilter)
                .eq(categoryId != null, Course::getCategoryId, categoryId)
                .eq(level != null, Course::getLevel, level)
                .eq(Boolean.TRUE.equals(freeOnly), Course::getIsFree, 1)
                .and(StringUtils.hasText(keyword), w -> w.like(Course::getTitle, keyword)
                        .or()
                        .like(Course::getSubtitle, keyword));

        if ("popular".equals(sortBy)) {
            q.orderByDesc(Course::getStudentCount);
        } else if ("rating".equals(sortBy)) {
            q.orderByDesc(Course::getRating);
        } else if (mineOnly) {
            // 草稿的 publishedAt 为空，按创建时间（雪花 id）倒序保证刚建的草稿排在最前
            q.orderByDesc(Course::getId);
        } else {
            q.orderByDesc(Course::getPublishedAt);
        }

        Page<Course> p = courseMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResponse.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(this::listVO).toList());
    }

    public CourseDetailVO getCourseDetail(Long id) {
        Course c = course(id);
        if (!pub(c) && !owner(c)) {
            throw new CourseException("课程未发布");
        }
        return detail(c);
    }

    public VideoPlayVO getVideoPlayInfo(Long id) {
        CourseVideo v = video(id);
        CourseChapter ch = chapter(v.getChapterId());
        Course c = course(ch.getCourseId());
        if ((!pub(c) || !Objects.equals(v.getStatus(),1)) && !owner(c)) throw new CourseException("课程或视频未发布");
        boolean pre = yes(v.getIsFreePreview()) || yes(ch.getIsFreePreview()) || yes(c.getIsFree());
        boolean ok = pre || canWatch(c);
        return VideoPlayVO.builder()
                .courseId(c.getId())
                .chapterId(ch.getId())
                .videoId(v.getId())
                .title(v.getVideoTitle())
                .videoUrl(ok ? v.getVideoUrl() : null)
                .freePreview(pre)
                .vipRequired(!pre)
                .playable(ok)
                .message(ok ? "可播放" : "该课程需要VIP才能观看")
                .build();
    }

    public CourseDetailVO createCourse(CreateCourseRequest r) {
        requireUploader(null);
        UserProfileVO instructor = currentUserProfile();
        Course c = new Course();
        c.setInstructorId(instructor.getUserId());
        c.setCategoryId(r.getCategoryId());
        c.setTitle(r.getTitle());
        c.setSubtitle(r.getSubtitle());
        c.setDescription(r.getDescription());
        c.setCoverUrl(r.getCoverUrl());
        c.setPrice(r.getPrice() == null ? BigDecimal.ZERO : r.getPrice());
        c.setOriginalPrice(r.getOriginalPrice() == null ? BigDecimal.ZERO : r.getOriginalPrice());
        c.setLevel(n(r.getLevel(), 1));
        c.setLanguage(StringUtils.hasText(r.getLanguage()) ? r.getLanguage() : "zh-CN");
        c.setIsFree(n(r.getIsFree(), 0));
        c.setStatus(0);
        courseMapper.insert(c);
        log.info("创建课程成功, courseId={}, title={}, instructorId={}",
                c.getId(), c.getTitle(), c.getInstructorId());
        return detail(c);
    }

    public CourseDetailVO updateCourse(Long id, UpdateCourseRequest r) {
        Course c = course(id);
        requireUploader(c);
        if (r.getCategoryId() != null) {
            c.setCategoryId(r.getCategoryId());
        }
        if (StringUtils.hasText(r.getTitle())) {
            c.setTitle(r.getTitle());
        }
        if (r.getSubtitle() != null) {
            c.setSubtitle(r.getSubtitle());
        }
        if (r.getDescription() != null) {
            c.setDescription(r.getDescription());
        }
        if (r.getCoverUrl() != null) {
            c.setCoverUrl(r.getCoverUrl());
        }
        if (r.getPrice() != null) {
            c.setPrice(r.getPrice());
        }
        if (r.getOriginalPrice() != null) {
            c.setOriginalPrice(r.getOriginalPrice());
        }
        if (r.getLevel() != null) {
            c.setLevel(r.getLevel());
        }
        if (StringUtils.hasText(r.getLanguage())) {
            c.setLanguage(r.getLanguage());
        }
        if (r.getIsFree() != null) {
            c.setIsFree(r.getIsFree());
        }
        if (r.getStatus() != null && manager()) {
            c.setStatus(r.getStatus());
        }
        courseMapper.updateById(c);
        log.info("更新课程成功, courseId={}", id);
        return detail(c);
    }

    public void publishCourse(Long id) {
        Course c = course(id);
        requireUploader(c);
        c.setStatus(1);
        c.setPublishedAt(LocalDateTime.now());
        courseMapper.updateById(c);
        log.info("发布课程成功, courseId={}, title={}", id, c.getTitle());
    }

    public void offlineCourse(Long id) {
        needManager();
        Course c = course(id);
        c.setStatus(2);
        courseMapper.updateById(c);
        log.info("下架课程, courseId={}", id);
    }

    public CourseChapterVO createChapter(CreateChapterRequest r) {
        Course c = course(r.getCourseId());
        requireUploader(c);
        CourseChapter ch = new CourseChapter();
        ch.setCourseId(r.getCourseId());
        ch.setChapterTitle(r.getChapterTitle());
        ch.setChapterDesc(r.getChapterDesc());
        ch.setSortOrder(n(r.getSortOrder(), 0));
        ch.setIsFreePreview(n(r.getIsFreePreview(), 0));
        chapterMapper.insert(ch);
        refresh(c.getId());
        log.info("创建课程章节成功, courseId={}, chapterId={}, title={}",
                c.getId(), ch.getId(), ch.getChapterTitle());
        return chapterVO(ch, List.of());
    }

    public CourseVideoUploadInitVO initVideoUpload(InitCourseVideoUploadRequest r) {
        CourseChapter ch = chapter(r.getChapterId());
        requireUploader(course(ch.getCourseId()));

        com.peakui.course.feign.dto.ChunkInitRequest x = new com.peakui.course.feign.dto.ChunkInitRequest();
        x.setOriginalName(r.getOriginalName());
        x.setFileType(r.getFileType() == null ? "video" : r.getFileType());
        x.setFileSize(r.getFileSize());
        x.setFileMd5(r.getFileMd5());
        x.setChunkSize(r.getChunkSize());
        x.setChunkCount(r.getChunkCount());
        x.setOwnerType(2);
        x.setIsPublic(0);

        var resp = fileFeignClient.initChunkUpload(x);
        if (resp == null || resp.getCode() != 0 || resp.getData() == null) {
            throw new CourseException(resp == null ? "初始化视频上传失败" : resp.getMessage());
        }
        var d = resp.getData();
        return CourseVideoUploadInitVO.builder()
                .fileId(d.getFileId())
                .uploadId(d.getUploadId())
                .chunkCount(d.getChunkCount())
                .chunkSize(d.getChunkSize())
                .uploadedChunks(d.getUploadedChunks())
                .chunkUploadUrl("/files/chunk/upload")
                .chunkStatusUrl("/files/chunk/status?fileId=" + d.getFileId())
                .completeUrl("/courses/videos/upload/complete")
                .build();
    }

    public CourseVideoVO completeVideoUpload(CompleteCourseVideoUploadRequest r) {
        CourseChapter ch = chapter(r.getChapterId());
        requireUploader(course(ch.getCourseId()));

        com.peakui.course.feign.dto.ChunkCompleteRequest x = new com.peakui.course.feign.dto.ChunkCompleteRequest();
        x.setFileId(r.getFileId());

        var resp = fileFeignClient.completeChunkUpload(x);
        if (resp == null || resp.getCode() != 0 || resp.getData() == null) {
            throw new CourseException(resp == null ? "完成视频上传失败" : resp.getMessage());
        }

        var signedResp = fileFeignClient.getSignedUrl(r.getFileId());
        if (signedResp == null || signedResp.getCode() != 0 || signedResp.getData() == null
                || !StringUtils.hasText(signedResp.getData().getSignedUrl())) {
            throw new CourseException(signedResp == null ? "获取视频签名地址失败" : signedResp.getMessage());
        }

        var f = resp.getData();
        Integer duration = videoMetadataService.parseDurationSeconds(signedResp.getData().getSignedUrl());
        CreateVideoRequest v = new CreateVideoRequest();
        v.setChapterId(r.getChapterId());
        v.setVideoTitle(r.getVideoTitle());
        v.setVideoDesc(r.getVideoDesc());
        v.setVideoUrl(f.getFileUrl());
        v.setCoverUrl(r.getCoverUrl());
        v.setDuration(duration);
        v.setFileSize(f.getFileSize());
        v.setResolution(r.getResolution());
        v.setSortOrder(r.getSortOrder());
        v.setIsFreePreview(r.getIsFreePreview());
        return createVideo(v);
    }

    public CourseVideoVO createVideo(CreateVideoRequest r) {
        CourseChapter ch = chapter(r.getChapterId());
        requireUploader(course(ch.getCourseId()));
        CourseVideo v = new CourseVideo();
        v.setChapterId(r.getChapterId());
        v.setVideoTitle(r.getVideoTitle());
        v.setVideoDesc(r.getVideoDesc());
        v.setVideoUrl(r.getVideoUrl());
        v.setCoverUrl(r.getCoverUrl());
        v.setDuration(n(r.getDuration(), 0));
        v.setFileSize(r.getFileSize() == null ? 0L : r.getFileSize());
        v.setResolution(r.getResolution());
        v.setSortOrder(n(r.getSortOrder(), 0));
        v.setIsFreePreview(n(r.getIsFreePreview(), 0));
        v.setStatus(1);
        videoMapper.insert(v);
        refresh(ch.getCourseId());
        log.info("创建课程视频成功, courseId={}, chapterId={}, videoId={}, title={}",
                ch.getCourseId(), r.getChapterId(), v.getId(), v.getVideoTitle());
        return videoVO(v);
    }

    public CourseVideoVO updateVideo(Long videoId, UpdateVideoRequest r) {
        CourseVideo v = video(videoId);
        CourseChapter oldChapter = chapter(v.getChapterId());
        requireUploader(course(oldChapter.getCourseId()));
        Long oldCourseId = oldChapter.getCourseId();
        Long newCourseId = oldCourseId;
        if (r.getChapterId() != null && !Objects.equals(r.getChapterId(), v.getChapterId())) {
            CourseChapter newChapter = chapter(r.getChapterId());
            requireUploader(course(newChapter.getCourseId()));
            v.setChapterId(r.getChapterId());
            newCourseId = newChapter.getCourseId();
        }
        if (StringUtils.hasText(r.getVideoTitle())) {
            v.setVideoTitle(r.getVideoTitle());
        }
        if (r.getVideoDesc() != null) {
            v.setVideoDesc(r.getVideoDesc());
        }
        if (StringUtils.hasText(r.getVideoUrl())) {
            v.setVideoUrl(r.getVideoUrl());
        }
        if (r.getCoverUrl() != null) {
            v.setCoverUrl(r.getCoverUrl());
        }
        if (r.getDuration() != null) {
            v.setDuration(r.getDuration());
        }
        if (r.getFileSize() != null) {
            v.setFileSize(r.getFileSize());
        }
        if (r.getResolution() != null) {
            v.setResolution(r.getResolution());
        }
        if (r.getSortOrder() != null) {
            v.setSortOrder(r.getSortOrder());
        }
        if (r.getIsFreePreview() != null) {
            v.setIsFreePreview(r.getIsFreePreview());
        }
        if (r.getStatus() != null) {
            v.setStatus(r.getStatus());
        }
        videoMapper.updateById(v);
        refresh(oldCourseId);
        if (!Objects.equals(oldCourseId, newCourseId)) {
            refresh(newCourseId);
        }
        log.info("更新课程视频成功, videoId={}, courseId={}", videoId, newCourseId);
        return videoVO(v);
    }

    public void deleteVideo(Long videoId) {
        CourseVideo v = video(videoId);
        CourseChapter ch = chapter(v.getChapterId());
        requireUploader(course(ch.getCourseId()));
        if (Objects.equals(v.getStatus(), 3)) {
            return;
        }
        v.setStatus(3);
        videoMapper.updateById(v);
        refresh(ch.getCourseId());
        log.info("删除课程视频, videoId={}, courseId={}", videoId, ch.getCourseId());
    }

    public CourseArticleVO createArticle(CreateArticleRequest r) {
        requireUploader(course(r.getCourseId()));
        CourseArticle a = new CourseArticle();
        a.setCourseId(r.getCourseId());
        a.setChapterId(r.getChapterId());
        a.setTitle(r.getTitle());
        a.setContent(r.getContent());
        a.setWordCount(r.getContent().length());
        a.setReadTime(Math.max(1, (int) Math.ceil(r.getContent().length() / 500.0)));
        a.setSortOrder(n(r.getSortOrder(), 0));
        a.setStatus(n(r.getStatus(), 1));
        articleMapper.insert(a);
        log.info("创建课程文章成功, courseId={}, articleId={}, title={}",
                r.getCourseId(), a.getId(), a.getTitle());
        return articleVO(a);
    }

    public CourseArticleVO updateArticle(Long articleId, UpdateArticleRequest r) {
        CourseArticle a = article(articleId);
        requireUploader(course(a.getCourseId()));
        if (r.getChapterId() != null) {
            CourseChapter ch = chapter(r.getChapterId());
            if (!Objects.equals(ch.getCourseId(), a.getCourseId())) {
                throw new CourseException("章节不属于当前课程");
            }
            a.setChapterId(r.getChapterId());
        }
        if (StringUtils.hasText(r.getTitle())) {
            a.setTitle(r.getTitle());
        }
        if (StringUtils.hasText(r.getContent())) {
            a.setContent(r.getContent());
            a.setWordCount(r.getContent().length());
            a.setReadTime(Math.max(1, (int) Math.ceil(r.getContent().length() / 500.0)));
        }
        if (r.getSortOrder() != null) {
            a.setSortOrder(r.getSortOrder());
        }
        if (r.getStatus() != null) {
            a.setStatus(r.getStatus());
        }
        a.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(a);
        log.info("更新课程文章成功, articleId={}, courseId={}", articleId, a.getCourseId());
        return articleVO(a);
    }

    public void deleteArticle(Long articleId) {
        CourseArticle a = article(articleId);
        requireUploader(course(a.getCourseId()));
        if (Objects.equals(a.getStatus(), 3)) {
            return;
        }
        a.setStatus(3);
        a.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(a);
        log.info("删除课程文章, articleId={}, courseId={}", articleId, a.getCourseId());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor=Exception.class)
    public CourseProgressVO updateProgress(Long courseId, UpdateProgressRequest r) {
        Long currentUser = uid();
        Course c = courseMapper.lockById(courseId);
        if(c == null) throw new CourseException("课程不存在");
        CourseVideo v = video(r.getVideoId());
        CourseChapter ch = chapter(v.getChapterId());
        if(!Objects.equals(ch.getCourseId(),courseId) || !Objects.equals(v.getStatus(),1) || (!pub(c) && !owner(c))) throw new CourseException("视频不属于可学习课程");
        if (!(yes(v.getIsFreePreview()) || yes(ch.getIsFreePreview()) || canWatch(c))) {
            throw new CourseException("该课程需要VIP才能学习");
        }
        CourseProgress p = progressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>().eq(CourseProgress::getUserId,currentUser)
                .eq(CourseProgress::getCourseId,courseId).eq(CourseProgress::getVideoId,r.getVideoId()).orderByDesc(CourseProgress::getLastWatchTime).last("LIMIT 1"));
        boolean creating = p == null;
        if(creating) p = new CourseProgress();
        p.setUserId(currentUser);
        p.setCourseId(courseId);
        p.setVideoId(r.getVideoId());
        p.setProgressPercent(p.getProgressPercent()==null?r.getProgressPercent():p.getProgressPercent().max(r.getProgressPercent()));
        p.setWatchedDuration(Math.max(n(p.getWatchedDuration(),0),Math.max(0,n(r.getWatchedDuration(),0))));
        p.setLastWatchTime(LocalDateTime.now());
        p.setIsCompleted(p.getProgressPercent().compareTo(BigDecimal.valueOf(100)) >= 0 ? 1 : 0);
        if (yes(p.getIsCompleted()) && p.getCompletedAt() == null) {
            p.setCompletedAt(LocalDateTime.now());
        }
        if(creating) progressMapper.insert(p); else progressMapper.updateById(p);
        log.info("更新课程学习进度, userId={}, courseId={}, videoId={}, percent={}, completed={}",
                currentUser, courseId, r.getVideoId(), p.getProgressPercent(), p.getIsCompleted());
        return progressVO(p);
    }

    public List<CourseProgressVO> listMyProgress(Long c) {
        return progressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                        .eq(CourseProgress::getUserId, uid())
                        .eq(CourseProgress::getCourseId, c))
                .stream()
                .map(this::progressVO)
                .toList();
    }

    public CourseNoteVO createNote(CreateNoteRequest r) {
        Long userId = uid();
        Course target = course(r.getCourseId());
        if ((!pub(target) && !owner(target)) || !canWatch(target)) {
            throw new CourseException("该课程需要VIP才能记笔记");
        }
        if (r.getVideoId() != null) {
            CourseVideo targetVideo = video(r.getVideoId());
            if (!Objects.equals(chapter(targetVideo.getChapterId()).getCourseId(), r.getCourseId())
                    || !Objects.equals(targetVideo.getStatus(), 1)) {
                throw new CourseException("视频不属于可学习课程");
            }
        }
        CourseNote x = new CourseNote();
        x.setUserId(userId);
        x.setCourseId(r.getCourseId());
        x.setVideoId(r.getVideoId());
        x.setContent(r.getContent());
        x.setTimestamp(n(r.getTimestamp(), 0));
        x.setIsPublic(n(r.getIsPublic(), 0));
        noteMapper.insert(x);
        log.info("创建课程笔记成功, noteId={}, userId={}, courseId={}",
                x.getId(), x.getUserId(), x.getCourseId());
        return noteVO(x);
    }

    public PageResponse<CourseNoteVO> listNotes(Long c, Long v, Boolean pub, Long pn, Long ps) {
        boolean publicOnly = Boolean.TRUE.equals(pub);
        Long userId = publicOnly ? null : uid();
        Course target = course(c);
        if ((!pub(target) && !owner(target)) || !canWatch(target)) {
            throw new CourseException("无权限查看课程笔记");
        }
        Page<CourseNote> p = noteMapper.selectPage(new Page<>(pn == null ? 1 : Math.max(1, pn), ps == null ? 10 : Math.max(1, Math.min(100, ps))),
                new LambdaQueryWrapper<CourseNote>()
                        .eq(CourseNote::getCourseId, c)
                        .eq(v != null, CourseNote::getVideoId, v)
                        .eq(publicOnly, CourseNote::getIsPublic, 1)
                        .eq(!publicOnly, CourseNote::getUserId, userId)
                        .orderByDesc(CourseNote::getCreatedAt));
        return PageResponse.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(this::noteVO).toList());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public CourseReviewVO createReview(CreateReviewRequest r) {
        Long userId = uid();
        Course target = courseMapper.lockById(r.getCourseId());
        if (target == null || !pub(target) || !canWatch(target)) throw new CourseException("无权限评价该课程");
        CourseReview x = reviewMapper.selectOne(new LambdaQueryWrapper<CourseReview>()
                .eq(CourseReview::getUserId, userId).eq(CourseReview::getCourseId, r.getCourseId()).last("LIMIT 1"));
        boolean creating = x == null;
        if (creating) x = new CourseReview();
        x.setUserId(userId);
        x.setCourseId(r.getCourseId());
        x.setRating(r.getRating());
        x.setContent(r.getContent());
        if (creating) x.setIsVerifiedPurchase(0);
        x.setStatus(1);
        if (creating) reviewMapper.insert(x); else reviewMapper.updateById(x);
        refreshRating(r.getCourseId());
        log.info("创建课程评价成功, reviewId={}, userId={}, courseId={}, rating={}",
                x.getId(), x.getUserId(), x.getCourseId(), x.getRating());
        return reviewVO(x);
    }

    public PageResponse<CourseReviewVO> listReviews(Long c, Long pn, Long ps) {
        Page<CourseReview> p = reviewMapper.selectPage(new Page<>(pn, ps),
                new LambdaQueryWrapper<CourseReview>()
                        .eq(CourseReview::getCourseId, c)
                        .eq(CourseReview::getStatus, 1)
                        .orderByDesc(CourseReview::getCreatedAt));
        return PageResponse.of(p.getCurrent(), p.getSize(), p.getTotal(),
                p.getRecords().stream().map(this::reviewVO).toList());
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public CourseCertificateVO issueCertificate(Long c) {
        Long userId = uid();
        Course target = courseMapper.lockById(c);
        if (target == null || !pub(target) || !canWatch(target)) {
            throw new CourseException("课程不存在或无学习权限");
        }
        CourseCertificate existing = certificateMapper.selectOne(new LambdaQueryWrapper<CourseCertificate>()
                .eq(CourseCertificate::getUserId, userId).eq(CourseCertificate::getCourseId, c).last("LIMIT 1"));
        if (existing != null) return certVO(existing);
        List<Long> chapterIds = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, c)).stream().map(CourseChapter::getId).toList();
        List<CourseVideo> videos = chapterIds.isEmpty() ? List.of() : videoMapper.selectList(new LambdaQueryWrapper<CourseVideo>()
                .in(CourseVideo::getChapterId, chapterIds).eq(CourseVideo::getStatus, 1));
        Set<Long> completed = progressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId).eq(CourseProgress::getCourseId, c)
                .eq(CourseProgress::getIsCompleted, 1)).stream().map(CourseProgress::getVideoId).collect(Collectors.toSet());
        if (videos.isEmpty() || videos.stream().anyMatch(v -> !completed.contains(v.getId()))) {
            throw new CourseException("请先完成课程全部视频，再领取证书");
        }
        CourseCertificate x = new CourseCertificate();
        x.setUserId(userId);
        x.setCourseId(c);
        x.setCertificateNo("COURSE-" + LocalDate.now().toString().replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        x.setIssueDate(LocalDate.now());
        x.setStatus(1);
        certificateMapper.insert(x);
        log.info("颁发课程证书成功, certificateNo={}, userId={}, courseId={}",
                x.getCertificateNo(), x.getUserId(), x.getCourseId());
        return certVO(x);
    }

    private CourseDetailVO detail(Course c) {
        boolean accessible = canWatch(c);
        List<CourseChapter> cs = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, c.getId())
                .orderByAsc(CourseChapter::getSortOrder));
        List<Long> ids = cs.stream().map(CourseChapter::getId).toList();
        Map<Long, List<CourseVideo>> vm = ids.isEmpty()
                ? Map.of()
                : videoMapper.selectList(new LambdaQueryWrapper<CourseVideo>()
                        .in(CourseVideo::getChapterId, ids)
                        .eq(CourseVideo::getStatus, 1)
                        .orderByAsc(CourseVideo::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(CourseVideo::getChapterId));
        List<CourseArticleVO> as = articleMapper.selectList(new LambdaQueryWrapper<CourseArticle>()
                        .eq(CourseArticle::getCourseId, c.getId())
                        .eq(CourseArticle::getStatus, 1)
                        .orderByAsc(CourseArticle::getSortOrder))
                .stream()
                .map(article -> { CourseArticleVO vo = articleVO(article); if(!accessible) vo.setContent(null); return vo; })
                .toList();
        UserProfileVO instructor = loadUserProfile(c.getInstructorId());
        return CourseDetailVO.builder()
                .id(c.getId())
                .instructorId(c.getInstructorId())
                .instructorName(instructor == null ? null : instructor.getUsername())
                .instructorNickname(instructor == null ? null : instructor.getNickname())
                .instructorAvatarUrl(instructor == null ? null : instructor.getAvatarUrl())
                .categoryId(c.getCategoryId())
                .title(c.getTitle())
                .subtitle(c.getSubtitle())
                .description(c.getDescription())
                .coverUrl(c.getCoverUrl())
                .price(c.getPrice())
                .originalPrice(c.getOriginalPrice())
                .level(c.getLevel())
                .language(c.getLanguage())
                .studentCount(c.getStudentCount())
                .chapterCount(c.getChapterCount())
                .videoCount(c.getVideoCount())
                .totalDuration(c.getTotalDuration())
                .rating(c.getRating())
                .ratingCount(c.getRatingCount())
                .isFree(c.getIsFree())
                .publishedAt(c.getPublishedAt())
                .vipRequired(!yes(c.getIsFree()))
                .canWatch(accessible)
                .chapters(cs.stream().map(x -> { var vo=chapterVO(x,vm.getOrDefault(x.getId(),List.of()));
                    // Detail exposes metadata only. Play URLs come from the authorized /play endpoint.
                    vo.getVideos().forEach(video -> video.setVideoUrl(null)); return vo; }).toList())
                .articles(as)
                .build();
    }

    private CourseListItemVO listVO(Course c) {
        UserProfileVO instructor = loadUserProfile(c.getInstructorId());
        return CourseListItemVO.builder()
                .id(c.getId())
                .instructorId(c.getInstructorId())
                .instructorName(instructor == null ? null : instructor.getUsername())
                .instructorNickname(instructor == null ? null : instructor.getNickname())
                .instructorAvatarUrl(instructor == null ? null : instructor.getAvatarUrl())
                .categoryId(c.getCategoryId())
                .title(c.getTitle())
                .subtitle(c.getSubtitle())
                .coverUrl(c.getCoverUrl())
                .price(c.getPrice())
                .level(c.getLevel())
                .studentCount(c.getStudentCount())
                .chapterCount(c.getChapterCount())
                .videoCount(c.getVideoCount())
                .totalDuration(c.getTotalDuration())
                .rating(c.getRating())
                .isFree(c.getIsFree())
                .publishedAt(c.getPublishedAt())
                .build();
    }

    private CourseChapterVO chapterVO(CourseChapter c, List<CourseVideo> vs) {
        return CourseChapterVO.builder()
                .id(c.getId())
                .courseId(c.getCourseId())
                .chapterTitle(c.getChapterTitle())
                .chapterDesc(c.getChapterDesc())
                .sortOrder(c.getSortOrder())
                .videoCount(c.getVideoCount())
                .duration(c.getDuration())
                .isFreePreview(c.getIsFreePreview())
                .videos(vs.stream().map(this::videoVO).toList())
                .build();
    }

    private CourseVideoVO videoVO(CourseVideo v) {
        return CourseVideoVO.builder()
                .id(v.getId())
                .chapterId(v.getChapterId())
                .videoTitle(v.getVideoTitle())
                .videoDesc(v.getVideoDesc())
                .videoUrl(v.getVideoUrl())
                .coverUrl(v.getCoverUrl())
                .duration(v.getDuration())
                .fileSize(v.getFileSize())
                .resolution(v.getResolution())
                .sortOrder(v.getSortOrder())
                .playCount(v.getPlayCount())
                .isFreePreview(v.getIsFreePreview())
                .build();
    }

    private CourseArticleVO articleVO(CourseArticle a) {
        return CourseArticleVO.builder()
                .id(a.getId())
                .courseId(a.getCourseId())
                .chapterId(a.getChapterId())
                .title(a.getTitle())
                .content(a.getContent())
                .wordCount(a.getWordCount())
                .readTime(a.getReadTime())
                .viewCount(a.getViewCount())
                .sortOrder(a.getSortOrder())
                .build();
    }

    private CourseProgressVO progressVO(CourseProgress p) {
        return CourseProgressVO.builder()
                .id(p.getId())
                .courseId(p.getCourseId())
                .videoId(p.getVideoId())
                .progressPercent(p.getProgressPercent())
                .watchedDuration(p.getWatchedDuration())
                .lastWatchTime(p.getLastWatchTime())
                .isCompleted(p.getIsCompleted())
                .completedAt(p.getCompletedAt())
                .build();
    }

    private CourseNoteVO noteVO(CourseNote n) {
        return CourseNoteVO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .courseId(n.getCourseId())
                .videoId(n.getVideoId())
                .content(n.getContent())
                .timestamp(n.getTimestamp())
                .isPublic(n.getIsPublic())
                .likeCount(n.getLikeCount())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private CourseReviewVO reviewVO(CourseReview r) {
        return CourseReviewVO.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .courseId(r.getCourseId())
                .rating(r.getRating())
                .content(r.getContent())
                .likeCount(r.getLikeCount())
                .isVerifiedPurchase(r.getIsVerifiedPurchase())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private CourseCertificateVO certVO(CourseCertificate c) {
        return CourseCertificateVO.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .courseId(c.getCourseId())
                .certificateNo(c.getCertificateNo())
                .certificateUrl(c.getCertificateUrl())
                .issueDate(c.getIssueDate())
                .expireDate(c.getExpireDate())
                .build();
    }

    private void refreshRating(Long courseId) {
        List<Integer> ratings = reviewMapper.selectList(new LambdaQueryWrapper<CourseReview>()
                        .eq(CourseReview::getCourseId, courseId)
                        .eq(CourseReview::getStatus, 1))
                .stream()
                .map(CourseReview::getRating)
                .filter(Objects::nonNull)
                .toList();
        Course c = course(courseId);
        c.setRatingCount(ratings.size());
        c.setRating(ratings.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(ratings.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0))
                .setScale(2, java.math.RoundingMode.HALF_UP));
        courseMapper.updateById(c);
    }

    private void refresh(Long id) {
        List<CourseChapter> cs = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, id));
        List<Long> ids = cs.stream().map(CourseChapter::getId).toList();
        List<CourseVideo> vs = ids.isEmpty()
                ? List.of()
                : videoMapper.selectList(new LambdaQueryWrapper<CourseVideo>()
                        .in(CourseVideo::getChapterId, ids)
                        .eq(CourseVideo::getStatus, 1));
        Course c = course(id);
        c.setChapterCount(cs.size());
        c.setVideoCount(vs.size());
        c.setTotalDuration(vs.stream()
                .map(CourseVideo::getDuration)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum));
        courseMapper.updateById(c);
    }

    private Course course(Long id) {
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new CourseException("课程不存在");
        }
        return c;
    }

    private CourseChapter chapter(Long id) {
        CourseChapter c = chapterMapper.selectById(id);
        if (c == null) {
            throw new CourseException("章节不存在");
        }
        return c;
    }

    private CourseVideo video(Long id) {
        CourseVideo v = videoMapper.selectById(id);
        if (v == null) {
            throw new CourseException("视频不存在");
        }
        return v;
    }

    private CourseArticle article(Long id) {
        CourseArticle a = articleMapper.selectById(id);
        if (a == null || Objects.equals(a.getStatus(), 3)) {
            throw new CourseException("文章不存在");
        }
        return a;
    }

    private boolean canWatch(Course c) {
        if(yes(c.getIsFree()) || owner(c)) return true;
        if(!StpUtil.isLogin()) return false;
        try {
            var response=entitlementClient.vip("Bearer " + StpUtil.getTokenValue());
            return response != null && response.getCode()==0 && response.getData()!=null && Boolean.TRUE.equals(response.getData().valid());
        } catch(Exception e) { return false; }
    }

    private void requireUploader(Course c) {
        uid();
        if (c != null && !owner(c)) {
            throw new CourseException("只能管理自己发布的课程");
        }
        if (!owner(c) && !role("teacher", "instructor", "verified", "certified")) {
            throw new CourseException("只有管理员、讲师或认证用户可以上传课程");
        }
    }

    private void needManager() {
        if (!manager()) {
            throw new CourseException("需要管理员权限");
        }
    }

    private boolean owner(Course c) {
        return StpUtil.isLogin() && (manager() || (c != null && Objects.equals(c.getInstructorId(), uid())));
    }

    private boolean manager() {
        return role("admin", "role_admin", "super_admin", "manager");
    }

    private boolean role(String... rs) {
        List<String> userRoles = parseRoles(request.getHeader(HEADER_USER_ROLES));
        if (userRoles.isEmpty() || rs == null || rs.length == 0) {
            return false;
        }
        for (String role : userRoles) {
            for (String expected : rs) {
                if (expected != null && role.equalsIgnoreCase(expected.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private UserProfileVO currentUserProfile() {
        UserProfileVO userProfile = loadUserProfile(uid());
        if (userProfile == null) {
            throw new CourseException("获取当前用户信息失败");
        }
        return userProfile;
    }

    private UserProfileVO loadUserProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            var response = userFeignClient.getUserProfile(userId);
            if (response == null || response.getData() == null) {
                return null;
            }
            return response.getData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long uid() {
        return StpUtil.getLoginIdAsLong();
    }

    private boolean yes(Integer v) {
        return v != null && v == 1;
    }

    private boolean pub(Course c) {
        return Objects.equals(c.getStatus(), 1);
    }

    private Integer n(Integer v, Integer d) {
        return v == null ? d : v;
    }
}
