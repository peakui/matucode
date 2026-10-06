package com.peakui.course.service;

import com.peakui.common.result.PageResponse;
import com.peakui.course.model.dto.CreateArticleRequest;
import com.peakui.course.model.dto.CreateChapterRequest;
import com.peakui.course.model.dto.CreateCourseRequest;
import com.peakui.course.model.dto.CreateNoteRequest;
import com.peakui.course.model.dto.CreateReviewRequest;
import com.peakui.course.model.dto.CreateVideoRequest;
import com.peakui.course.model.dto.UpdateArticleRequest;
import com.peakui.course.model.dto.UpdateCourseRequest;
import com.peakui.course.model.dto.UpdateProgressRequest;
import com.peakui.course.model.dto.UpdateVideoRequest;
import com.peakui.course.model.vo.CourseArticleVO;
import com.peakui.course.model.vo.CourseCertificateVO;
import com.peakui.course.model.vo.CourseChapterVO;
import com.peakui.course.model.vo.CourseDetailVO;
import com.peakui.course.model.vo.CourseListItemVO;
import com.peakui.course.model.vo.CourseNoteVO;
import com.peakui.course.model.vo.CourseProgressVO;
import com.peakui.course.model.vo.CourseReviewVO;
import com.peakui.course.model.vo.CourseVideoUploadInitVO;
import com.peakui.course.model.vo.CourseVideoVO;
import com.peakui.course.model.vo.VideoPlayVO;

import com.peakui.course.model.dto.CompleteCourseVideoUploadRequest;
import com.peakui.course.model.dto.InitCourseVideoUploadRequest;

import java.util.List;

public interface CourseService {
    PageResponse<CourseArticleVO> listPublishedArticles(String keyword, Long pageNum, Long pageSize);
    PageResponse<CourseListItemVO> listCourses(Integer status, Long categoryId, Integer level, Boolean freeOnly, String keyword, Long pageNum, Long pageSize, String sortBy, Boolean mine);

    CourseDetailVO getCourseDetail(Long courseId);

    VideoPlayVO getVideoPlayInfo(Long videoId);

    CourseDetailVO createCourse(CreateCourseRequest request);

    CourseDetailVO updateCourse(Long courseId, UpdateCourseRequest request);

    void publishCourse(Long courseId);

    void offlineCourse(Long courseId);

    CourseChapterVO createChapter(CreateChapterRequest request);

    CourseVideoUploadInitVO initVideoUpload(InitCourseVideoUploadRequest request);

    CourseVideoVO completeVideoUpload(CompleteCourseVideoUploadRequest request);

    CourseVideoVO createVideo(CreateVideoRequest request);

    CourseVideoVO updateVideo(Long videoId, UpdateVideoRequest request);

    void deleteVideo(Long videoId);

    CourseArticleVO createArticle(CreateArticleRequest request);

    CourseArticleVO updateArticle(Long articleId, UpdateArticleRequest request);

    void deleteArticle(Long articleId);

    CourseProgressVO updateProgress(Long courseId, UpdateProgressRequest request);

    List<CourseProgressVO> listMyProgress(Long courseId);

    CourseNoteVO createNote(CreateNoteRequest request);

    PageResponse<CourseNoteVO> listNotes(Long courseId, Long videoId, Boolean onlyPublic, Long pageNum, Long pageSize);

    CourseReviewVO createReview(CreateReviewRequest request);

    PageResponse<CourseReviewVO> listReviews(Long courseId, Long pageNum, Long pageSize);

    CourseCertificateVO issueCertificate(Long courseId);
}
