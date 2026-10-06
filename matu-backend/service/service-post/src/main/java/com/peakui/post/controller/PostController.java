package com.peakui.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.common.dashboard.DashboardPostStatsVO;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.post.mapper.PostMapper;
import com.peakui.post.model.dto.CreateCommentRequest;
import com.peakui.post.model.dto.ApplyAiSummaryCommentRequest;
import com.peakui.post.model.dto.CreatePostRequest;
import com.peakui.post.model.dto.CreateReportRequest;
import com.peakui.post.model.dto.UpdatePostRequest;
import com.peakui.post.model.entity.Post;
import com.peakui.post.model.vo.CommentVO;
import com.peakui.post.model.vo.PostCategoryVO;
import com.peakui.post.model.vo.PostDetailVO;
import com.peakui.post.model.vo.PostListItemVO;
import com.peakui.post.model.vo.PostTagVO;
import com.peakui.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 帖子控制器。
 */
@Tag(name = "帖子接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;

    @Value("${post.ai.internal-token:}")
    private String aiInternalToken;

    @Operation(summary = "帖子数据总览统计")
    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardPostStatsVO> dashboardStats() {
        Long articleCount = postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 1));
        return ApiResponse.success(DashboardPostStatsVO.builder().articleCount(articleCount).build());
    }

    @Operation(summary = "发布帖子")
    @PostMapping
    public ApiResponse<PostDetailVO> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.success(CommonError.PUBLISH_SUCCESS.message(), postService.createPost(request));
    }

    @Operation(summary = "更新帖子")
    @PutMapping("/{postId}")
    public ApiResponse<PostDetailVO> updatePost(@PathVariable Long postId,
                                                @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), postService.updatePost(postId, request));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailVO> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPostDetail(postId));
    }

    @Operation(summary = "帖子列表")
    @GetMapping
    public ApiResponse<PageResponse<PostListItemVO>> listPosts(@RequestParam(required = false) Long categoryId,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) Long userId,
                                                               @RequestParam(defaultValue = "1") Long pageNum,
                                                               @RequestParam(defaultValue = "10") Long pageSize,
                                                               @RequestParam(defaultValue = "latest") String sortBy) {
        return ApiResponse.success(postService.listPosts(categoryId, status, keyword, userId, pageNum, pageSize, sortBy));
    }

    @Operation(summary = "点赞帖子")
    @PostMapping("/{postId}/like")
    public ApiResponse<Void> likePost(@PathVariable Long postId) {
        postService.likePost(postId);
        return ApiResponse.success(CommonError.LIKE_SUCCESS.message(), null);
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{postId}/like")
    public ApiResponse<Void> unlikePost(@PathVariable Long postId) {
        postService.unlikePost(postId);
        return ApiResponse.success(CommonError.UNLIKE_SUCCESS.message(), null);
    }

    @Operation(summary = "收藏帖子")
    @PostMapping("/{postId}/collect")
    public ApiResponse<Void> collectPost(@PathVariable Long postId,
                                         @RequestParam(required = false) Long collectFolderId) {
        postService.collectPost(postId, collectFolderId);
        return ApiResponse.success(CommonError.COLLECT_SUCCESS.message(), null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{postId}/collect")
    public ApiResponse<Void> uncollectPost(@PathVariable Long postId) {
        postService.uncollectPost(postId);
        return ApiResponse.success(CommonError.UNCOLLECT_SUCCESS.message(), null);
    }

    @Operation(summary = "分享帖子")
    @PostMapping("/{postId}/share")
    public ApiResponse<Void> sharePost(@PathVariable Long postId) {
        postService.sharePost(postId);
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), null);
    }

    @Operation(summary = "AI 更新文章总结并发表评论")
    @PostMapping("/internal/ai/{postId}/summary-comment")
    public ApiResponse<Void> applyAiSummaryComment(@PathVariable Long postId,
                                                     @Valid @RequestBody ApplyAiSummaryCommentRequest request,
                                                     @RequestHeader(value = "X-AI-Service-Token", required = false) String token) {
        verifyAiServiceToken(token);
        postService.applyAiSummaryAndComment(postId, request);
        return ApiResponse.success("AI 总结和评论已保存", null);
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{postId}/comments")
    public ApiResponse<CommentVO> createComment(@PathVariable Long postId,
                                                @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.success(CommonError.COMMENT_SUCCESS.message(), postService.createComment(postId, request));
    }

    @Operation(summary = "评论列表")
    @GetMapping("/{postId}/comments")
    public ApiResponse<List<CommentVO>> listComments(@PathVariable Long postId) {
        return ApiResponse.success(postService.listComments(postId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long postId, @PathVariable Long commentId) {
        postService.deleteComment(postId, commentId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{postId}/comments/{commentId}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long postId, @PathVariable Long commentId) {
        postService.likeComment(postId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{postId}/comments/{commentId}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long postId, @PathVariable Long commentId) {
        postService.unlikeComment(postId, commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "举报帖子")
    @PostMapping("/{postId}/reports")
    public ApiResponse<Void> reportPost(@PathVariable Long postId,
                                        @Valid @RequestBody CreateReportRequest request) {
        postService.reportPost(postId, request);
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), null);
    }

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<PostCategoryVO>> listCategories() {
        return ApiResponse.success(postService.listCategories());
    }

    @Operation(summary = "我的草稿列表")
    @GetMapping("/drafts")
    public ApiResponse<PageResponse<PostListItemVO>> listDrafts(@RequestParam(defaultValue = "1") Long pageNum,
                                                                @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(postService.listDrafts(pageNum, pageSize));
    }

    @Operation(summary = "我收藏的帖子")
    @GetMapping("/collected")
    public ApiResponse<PageResponse<PostListItemVO>> listCollectedPosts(@RequestParam(defaultValue = "1") Long pageNum,
                                                                        @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(postService.listCollectedPosts(pageNum, pageSize));
    }

    @Operation(summary = "我点赞的帖子")
    @GetMapping("/liked")
    public ApiResponse<PageResponse<PostListItemVO>> listLikedPosts(@RequestParam(defaultValue = "1") Long pageNum,
                                                                    @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(postService.listLikedPosts(pageNum, pageSize));
    }

    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public ApiResponse<List<PostTagVO>> listTags(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(postService.listTags(keyword));
    }

    private void verifyAiServiceToken(String provided) {
        if (!org.springframework.util.StringUtils.hasText(aiInternalToken)
                || !org.springframework.util.StringUtils.hasText(provided)
                || !MessageDigest.isEqual(aiInternalToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无效的 AI 服务凭证");
        }
    }
}
