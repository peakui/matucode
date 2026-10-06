package com.peakui.post.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.security.PostContentFingerprint;
import com.peakui.post.feign.AuthFeignClient;
import com.peakui.post.mapper.*;
import com.peakui.post.model.dto.ApplyAiSummaryCommentRequest;
import com.peakui.post.model.entity.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostAiSummaryCommentTest {
    @Mock PostMapper posts;
    @Mock PostContentMapper contents;
    @Mock CommentMapper comments;
    @Mock AuthFeignClient auth;
    @InjectMocks PostServiceImpl service;
    private Post post;
    private ApplyAiSummaryCommentRequest request;

    @BeforeEach void setup() {
        MockitoAnnotations.openMocks(this);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        for (Class<?> type : new Class[]{Post.class, PostContent.class, Comment.class}) {
            TableInfoHelper.initTableInfo(assistant, type);
        }
        ReflectionTestUtils.setField(service, "aiCommentUserId", 99L);
        post = new Post();
        post.setId(42L); post.setTitle("title"); post.setStatus(1); post.setIsLock(0);
        when(posts.selectOne(any(Wrapper.class))).thenReturn(post);
        PostContent content = new PostContent(); content.setContent("body");
        when(contents.selectOne(any(Wrapper.class))).thenReturn(content);
        var account = new AuthFeignClient.UserProfileResponse();
        account.setUserId(99L); account.setUsername("matu_ai"); account.setStatus(1);
        when(auth.getUserProfile(99L)).thenReturn(ApiResponse.success(account));
        request = new ApplyAiSummaryCommentRequest();
        request.setSummary("摘要"); request.setComment("摘要");
        request.setSourceHash(PostContentFingerprint.of("title", "body"));
    }

    @Test void insertsWithAiIdentityAndIncrementsCounter() {
        service.applyAiSummaryAndComment(42L, request);
        verify(comments).insert(argThat((Comment c) -> c.getUserId() == 99L && c.getContent().contains("AI 自动生成")));
        verify(posts).update(isNull(), any(Wrapper.class));
        verify(posts, never()).updateById(any(Post.class)); // Preserve the author's own summary.
    }

    @Test void duplicateDoesNotInsertOrIncrementAgain() {
        Comment existing = new Comment(); existing.setId(7L); existing.setStatus(1);
        existing.setContent("📌 文章摘要（AI 自动生成）：\n摘要");
        when(comments.selectOne(any(Wrapper.class))).thenReturn(existing);
        service.applyAiSummaryAndComment(42L, request);
        verify(comments, never()).insert(any(Comment.class));
        verify(posts, never()).update(isNull(), any(Wrapper.class));
    }

    @Test void staleResultDoesNotWriteOrLookupAccount() {
        request.setSourceHash("0".repeat(64));
        service.applyAiSummaryAndComment(42L, request);
        verifyNoInteractions(comments, auth);
    }

    @Test void unpublishedOrLockedPostIsSkipped() {
        post.setStatus(0);
        service.applyAiSummaryAndComment(42L, request);
        post.setStatus(1); post.setIsLock(1);
        service.applyAiSummaryAndComment(42L, request);
        verifyNoInteractions(comments, auth, contents);
    }

    @Test void deletedAiCommentIsNotResurrected() {
        Comment existing = new Comment(); existing.setId(7L); existing.setStatus(3);
        when(comments.selectOne(any(Wrapper.class))).thenReturn(existing);
        service.applyAiSummaryAndComment(42L, request);
        verify(comments, never()).insert(any(Comment.class));
        verify(comments, never()).update(isNull(), any(Wrapper.class));
    }

    @Test void missingAccountFailsBeforeInsert() {
        when(auth.getUserProfile(99L)).thenReturn(ApiResponse.fail(404, "missing"));
        assertThrows(RuntimeException.class, () -> service.applyAiSummaryAndComment(42L, request));
        verifyNoInteractions(comments);
    }
}
