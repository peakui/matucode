package com.peakui.course.service;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.common.result.ApiResponse;
import com.peakui.course.feign.*;
import com.peakui.course.mapper.*;
import com.peakui.course.model.entity.*;
import com.peakui.course.model.dto.UpdateProgressRequest;
import com.peakui.course.model.dto.UpdateCourseRequest;
import com.peakui.course.model.dto.CreateNoteRequest;
import com.peakui.course.service.impl.CourseServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseMobileAccessTest {
 @Mock CourseMapper courseMapper; @Mock CourseChapterMapper chapterMapper; @Mock CourseVideoMapper videoMapper;
 @Mock CourseProgressMapper progressMapper; @Mock CourseNoteMapper noteMapper; @Mock CourseReviewMapper reviewMapper;
 @Mock CourseCertificateMapper certificateMapper; @Mock CourseArticleMapper articleMapper;
 @Mock FileFeignClient fileFeignClient; @Mock UserFeignClient userFeignClient; @Mock VideoMetadataService videoMetadataService;
 @Mock HttpServletRequest request; @Mock CourseEntitlementClient entitlementClient;
 @InjectMocks CourseServiceImpl service;
 private Course course(int free){Course c=new Course();c.setId(1L);c.setInstructorId(11L);c.setIsFree(free);c.setStatus(1);return c;}
 private CourseChapter chapter(){CourseChapter c=new CourseChapter();c.setId(2L);c.setCourseId(1L);return c;}
 private CourseVideo video(){CourseVideo v=new CourseVideo();v.setId(3L);v.setChapterId(2L);v.setStatus(1);v.setVideoUrl("https://example.test/private.mp4");return v;}
 @Test void anonymousDetailNeverContainsPaidBodyOrVideoUrl(){
  try(var stp=mockStatic(StpUtil.class)){
   when(courseMapper.selectById(1L)).thenReturn(course(0));when(chapterMapper.selectList(any())).thenReturn(List.of(chapter()));
   when(videoMapper.selectList(any())).thenReturn(List.of(video()));CourseArticle a=new CourseArticle();a.setId(4L);a.setContent("PAID BODY");when(articleMapper.selectList(any())).thenReturn(List.of(a));
   var result=service.getCourseDetail(1L);assertFalse(result.getCanWatch());assertNull(result.getArticles().get(0).getContent());assertNull(result.getChapters().get(0).getVideos().get(0).getVideoUrl());
  }
 }
 @Test void expiredVipRoleCannotUnlockVideo(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::isLogin).thenReturn(true);stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);stp.when(StpUtil::getTokenValue).thenReturn("test-token");
   when(request.getHeader("X-User-Roles")).thenReturn("VIP");when(videoMapper.selectById(3L)).thenReturn(video());when(chapterMapper.selectById(2L)).thenReturn(chapter());when(courseMapper.selectById(1L)).thenReturn(course(0));
   when(entitlementClient.vip("Bearer test-token")).thenReturn(ApiResponse.success(new CourseEntitlementClient.VipAccess(false)));
   var result=service.getVideoPlayInfo(3L);assertFalse(result.getPlayable());assertNull(result.getVideoUrl());
  }
 }
 @Test void activePcVipCanWatchThroughSharedEntitlement(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::isLogin).thenReturn(true);stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);stp.when(StpUtil::getTokenValue).thenReturn("test-token");
   when(videoMapper.selectById(3L)).thenReturn(video());when(chapterMapper.selectById(2L)).thenReturn(chapter());when(courseMapper.selectById(1L)).thenReturn(course(0));when(entitlementClient.vip("Bearer test-token")).thenReturn(ApiResponse.success(new CourseEntitlementClient.VipAccess(true)));
   assertTrue(service.getVideoPlayInfo(3L).getPlayable());
  }
 }
 @Test void unpublishedFreeCourseStillCannotPlay(){
  try(var stp=mockStatic(StpUtil.class)){
   Course draft=course(1);draft.setStatus(0);when(videoMapper.selectById(3L)).thenReturn(video());when(chapterMapper.selectById(2L)).thenReturn(chapter());when(courseMapper.selectById(1L)).thenReturn(draft);
   assertThrows(RuntimeException.class,()->service.getVideoPlayInfo(3L));
  }
 }
 @Test void progressUpdatesExistingRecordAndNeverMovesBackwards(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.lockById(1L)).thenReturn(course(1));when(videoMapper.selectById(3L)).thenReturn(video());when(chapterMapper.selectById(2L)).thenReturn(chapter());
   CourseProgress prior=new CourseProgress();prior.setId(9L);prior.setProgressPercent(BigDecimal.valueOf(80));prior.setWatchedDuration(80);when(progressMapper.selectOne(any())).thenReturn(prior);
   UpdateProgressRequest r=new UpdateProgressRequest();r.setVideoId(3L);r.setProgressPercent(BigDecimal.valueOf(20));r.setWatchedDuration(20);
   var saved=service.updateProgress(1L,r);assertEquals(80,saved.getWatchedDuration());assertEquals(BigDecimal.valueOf(80),saved.getProgressPercent());verify(progressMapper).updateById(prior);verify(progressMapper,never()).insert(any(CourseProgress.class));
  }
 }
 @Test void rejectsProgressForVideoFromAnotherCourse(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.lockById(1L)).thenReturn(course(1));when(videoMapper.selectById(3L)).thenReturn(video());CourseChapter other=chapter();other.setCourseId(999L);when(chapterMapper.selectById(2L)).thenReturn(other);
   UpdateProgressRequest r=new UpdateProgressRequest();r.setVideoId(3L);r.setProgressPercent(BigDecimal.TEN);assertThrows(RuntimeException.class,()->service.updateProgress(1L,r));verifyNoInteractions(progressMapper);
  }
 }
 @Test void teacherCannotEditAnotherTeachersCourse(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::isLogin).thenReturn(true);stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);
   when(request.getHeader("X-User-Roles")).thenReturn("TEACHER");when(courseMapper.selectById(1L)).thenReturn(course(1));
   assertThrows(RuntimeException.class,()->service.updateCourse(1L,new UpdateCourseRequest()));
   verify(courseMapper,never()).updateById(any(Course.class));
  }
 }
 @Test void notesRejectVideoFromAnotherCourse(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.selectById(1L)).thenReturn(course(1));
   when(videoMapper.selectById(3L)).thenReturn(video());CourseChapter other=chapter();other.setCourseId(999L);when(chapterMapper.selectById(2L)).thenReturn(other);
   CreateNoteRequest r=new CreateNoteRequest();r.setCourseId(1L);r.setVideoId(3L);r.setContent("private note");
   assertThrows(RuntimeException.class,()->service.createNote(r));verifyNoInteractions(noteMapper);
  }
 }
 @Test void privateNotesAreAlwaysFilteredByLoggedInOwner(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.selectById(1L)).thenReturn(course(1));
   var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
   configuration.setMapUnderscoreToCamelCase(true);
   com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "test"),CourseNote.class);
   when(noteMapper.selectPage(any(),any())).thenAnswer(invocation->{
    com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CourseNote> query=invocation.getArgument(1);
    assertTrue(query.getSqlSegment().contains("user_id ="));assertTrue(query.getParamNameValuePairs().containsValue(22L));
    return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<CourseNote>(1,10);
   });
   service.listNotes(1L,null,false,1L,10L);
  }
 }
 @Test void certificateRequiresEveryPublishedVideoCompleted(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.lockById(1L)).thenReturn(course(1));
   when(chapterMapper.selectList(any())).thenReturn(List.of(chapter()));when(videoMapper.selectList(any())).thenReturn(List.of(video()));when(progressMapper.selectList(any())).thenReturn(List.of());
   assertThrows(RuntimeException.class,()->service.issueCertificate(1L));verify(certificateMapper,never()).insert(any(CourseCertificate.class));
  }
 }
 @Test void certificateIsIdempotent(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.lockById(1L)).thenReturn(course(1));
   CourseCertificate cert=new CourseCertificate();cert.setCertificateNo("existing");when(certificateMapper.selectOne(any())).thenReturn(cert);
   assertEquals("existing",service.issueCertificate(1L).getCertificateNo());verify(certificateMapper,never()).insert(any(CourseCertificate.class));verifyNoInteractions(progressMapper);
  }
 }
 @Test void completedCourseCanIssueCertificate(){
  try(var stp=mockStatic(StpUtil.class)){
   stp.when(StpUtil::getLoginIdAsLong).thenReturn(22L);when(courseMapper.lockById(1L)).thenReturn(course(1));
   when(chapterMapper.selectList(any())).thenReturn(List.of(chapter()));when(videoMapper.selectList(any())).thenReturn(List.of(video()));
   CourseProgress progress=new CourseProgress();progress.setVideoId(3L);progress.setIsCompleted(1);when(progressMapper.selectList(any())).thenReturn(List.of(progress));
   assertNotNull(service.issueCertificate(1L).getCertificateNo());verify(certificateMapper).insert(any(CourseCertificate.class));
  }
 }
}
