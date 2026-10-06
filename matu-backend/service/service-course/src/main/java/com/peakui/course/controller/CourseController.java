package com.peakui.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.common.dashboard.DashboardCourseStatsDTO;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.course.mapper.CourseArticleMapper;
import com.peakui.course.mapper.CourseCertificateMapper;
import com.peakui.course.mapper.CourseMapper;
import com.peakui.course.mapper.CourseProgressMapper;
import com.peakui.course.mapper.CourseVideoMapper;
import com.peakui.course.model.dto.*;
import com.peakui.course.model.entity.Course;
import com.peakui.course.model.entity.CourseArticle;
import com.peakui.course.model.entity.CourseCertificate;
import com.peakui.course.model.entity.CourseProgress;
import com.peakui.course.model.entity.CourseVideo;
import com.peakui.course.model.vo.*;
import com.peakui.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "课程接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {
    private final CourseService courseService;
    private final CourseMapper courseMapper;
    private final CourseProgressMapper courseProgressMapper;
    private final CourseVideoMapper courseVideoMapper;
    private final CourseArticleMapper courseArticleMapper;
    private final CourseCertificateMapper courseCertificateMapper;

    @GetMapping("/articles/published")
    public ApiResponse<PageResponse<CourseArticleVO>> publishedArticles(@RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="1") Long pageNum,@RequestParam(defaultValue="12") Long pageSize) {
        return ApiResponse.success(courseService.listPublishedArticles(keyword,pageNum,pageSize));
    }

    @Operation(summary = "课程数据总览统计")
    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardCourseStatsDTO> dashboardStats() {
        return ApiResponse.success(DashboardCourseStatsDTO.builder()
                .courseCount(courseMapper.selectCount(null))
                .publishedCourseCount(courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1)))
                .studentCount(courseProgressMapper.countDistinctStudents())
                .videoCount(courseVideoMapper.selectCount(new LambdaQueryWrapper<CourseVideo>().eq(CourseVideo::getStatus, 1)))
                .articleCount(courseArticleMapper.selectCount(new LambdaQueryWrapper<CourseArticle>().eq(CourseArticle::getStatus, 1)))
                .certificateCount(courseCertificateMapper.selectCount(new LambdaQueryWrapper<CourseCertificate>().eq(CourseCertificate::getStatus, 1)))
                .build());
    }

    @Operation(summary = "课程列表")
    @GetMapping
    public ApiResponse<PageResponse<CourseListItemVO>> listCourses(@RequestParam(required = false) Integer status,
                                                                   @RequestParam(required = false) Long categoryId,
                                                                   @RequestParam(required = false) Integer level,
                                                                   @RequestParam(required = false) Boolean freeOnly,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(defaultValue = "1") Long pageNum,
                                                                   @RequestParam(defaultValue = "10") Long pageSize,
                                                                   @RequestParam(defaultValue = "latest") String sortBy,
                                                                   @RequestParam(required = false) Boolean mine) {
        return ApiResponse.success(courseService.listCourses(status, categoryId, level, freeOnly, keyword, pageNum, pageSize, sortBy, mine));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> detail(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.getCourseDetail(courseId));
    }

    @Operation(summary = "获取视频播放信息，非试看课程需要VIP")
    @GetMapping("/videos/{videoId}/play")
    public ApiResponse<VideoPlayVO> play(@PathVariable Long videoId) {
        return ApiResponse.success(courseService.getVideoPlayInfo(videoId));
    }

    @Operation(summary = "创建课程，管理员/讲师/认证用户可用")
    @PostMapping
    public ApiResponse<CourseDetailVO> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), courseService.createCourse(request));
    }

    @Operation(summary = "更新课程")
    @PutMapping("/{courseId}")
    public ApiResponse<CourseDetailVO> updateCourse(@PathVariable Long courseId, @Valid @RequestBody UpdateCourseRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), courseService.updateCourse(courseId, request));
    }

    @Operation(summary = "发布课程")
    @PostMapping("/{courseId}/publish")
    public ApiResponse<Void> publish(@PathVariable Long courseId) {
        courseService.publishCourse(courseId);
        return ApiResponse.success(CommonError.PUBLISH_SUCCESS.message(), null);
    }

    @Operation(summary = "下架课程，仅管理员可用")
    @PostMapping("/{courseId}/offline")
    public ApiResponse<Void> offline(@PathVariable Long courseId) {
        courseService.offlineCourse(courseId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "创建章节")
    @PostMapping("/chapters")
    public ApiResponse<CourseChapterVO> createChapter(@Valid @RequestBody CreateChapterRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), courseService.createChapter(request));
    }

    @Operation(summary = "初始化课程视频断点续传")
    @PostMapping("/videos/upload/init")
    public ApiResponse<CourseVideoUploadInitVO> initVideoUpload(@Valid @RequestBody InitCourseVideoUploadRequest request) {
        return ApiResponse.success(courseService.initVideoUpload(request));
    }

    @Operation(summary = "完成课程视频断点续传并创建视频")
    @PostMapping("/videos/upload/complete")
    public ApiResponse<CourseVideoVO> completeVideoUpload(@Valid @RequestBody CompleteCourseVideoUploadRequest request) {
        return ApiResponse.success(CommonError.UPLOAD_SUCCESS.message(), courseService.completeVideoUpload(request));
    }

    @Operation(summary = "上传/登记课程视频")
    @PostMapping("/videos")
    public ApiResponse<CourseVideoVO> createVideo(@Valid @RequestBody CreateVideoRequest request) {
        return ApiResponse.success(CommonError.UPLOAD_SUCCESS.message(), courseService.createVideo(request));
    }

    @Operation(summary = "更新课程视频")
    @PutMapping("/videos/{videoId}")
    public ApiResponse<CourseVideoVO> updateVideo(@PathVariable Long videoId, @RequestBody UpdateVideoRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), courseService.updateVideo(videoId, request));
    }

    @Operation(summary = "删除课程视频")
    @DeleteMapping("/videos/{videoId}")
    public ApiResponse<Void> deleteVideo(@PathVariable Long videoId) {
        courseService.deleteVideo(videoId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "创建文字教程")
    @PostMapping("/articles")
    public ApiResponse<CourseArticleVO> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), courseService.createArticle(request));
    }

    @Operation(summary = "更新文字教程")
    @PutMapping("/articles/{articleId}")
    public ApiResponse<CourseArticleVO> updateArticle(@PathVariable Long articleId, @RequestBody UpdateArticleRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), courseService.updateArticle(articleId, request));
    }

    @Operation(summary = "删除文字教程")
    @DeleteMapping("/articles/{articleId}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long articleId) {
        courseService.deleteArticle(articleId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "更新学习进度")
    @PostMapping("/{courseId}/progress")
    public ApiResponse<CourseProgressVO> updateProgress(@PathVariable Long courseId, @Valid @RequestBody UpdateProgressRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), courseService.updateProgress(courseId, request));
    }

    @Operation(summary = "我的课程进度")
    @GetMapping("/{courseId}/progress/mine")
    public ApiResponse<List<CourseProgressVO>> myProgress(@PathVariable Long courseId) {
        return ApiResponse.success(courseService.listMyProgress(courseId));
    }

    @Operation(summary = "创建学习笔记")
    @PostMapping("/notes")
    public ApiResponse<CourseNoteVO> createNote(@Valid @RequestBody CreateNoteRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), courseService.createNote(request));
    }

    @Operation(summary = "学习笔记列表")
    @GetMapping("/notes")
    public ApiResponse<PageResponse<CourseNoteVO>> notes(@RequestParam Long courseId,
                                                         @RequestParam(required = false) Long videoId,
                                                         @RequestParam(defaultValue = "true") Boolean onlyPublic,
                                                         @RequestParam(defaultValue = "1") Long pageNum,
                                                         @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(courseService.listNotes(courseId, videoId, onlyPublic, pageNum, pageSize));
    }

    @Operation(summary = "发表评价")
    @PostMapping("/reviews")
    public ApiResponse<CourseReviewVO> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.success(CommonError.COMMENT_SUCCESS.message(), courseService.createReview(request));
    }

    @Operation(summary = "课程评价列表")
    @GetMapping("/{courseId}/reviews")
    public ApiResponse<PageResponse<CourseReviewVO>> reviews(@PathVariable Long courseId,
                                                             @RequestParam(defaultValue = "1") Long pageNum,
                                                             @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(courseService.listReviews(courseId, pageNum, pageSize));
    }

    @Operation(summary = "发放课程证书")
    @PostMapping("/{courseId}/certificate")
    public ApiResponse<CourseCertificateVO> certificate(@PathVariable Long courseId) {
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), courseService.issueCertificate(courseId));
    }
}
