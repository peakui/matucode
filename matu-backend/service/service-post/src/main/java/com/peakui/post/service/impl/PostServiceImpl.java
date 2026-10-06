package com.peakui.post.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.post.exception.PostException;
import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeMutationResult;
import com.peakui.post.mq.LikeEvent;
import com.peakui.post.mq.LikeEventPublisher;
import com.peakui.post.mq.PostPublishedEvent;
import com.peakui.common.security.PostContentFingerprint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.peakui.post.feign.AuthFeignClient;
import com.peakui.post.feign.MessageFeignClient;
import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.post.mapper.CommentLikeMapper;
import com.peakui.post.mapper.CommentMapper;
import com.peakui.post.mapper.PostCategoryMapper;
import com.peakui.post.mapper.PostCollectMapper;
import com.peakui.post.mapper.PostContentMapper;
import com.peakui.post.mapper.PostEditHistoryMapper;
import com.peakui.post.mapper.PostImageMapper;
import com.peakui.post.mapper.PostLikeMapper;
import com.peakui.post.mapper.PostMapper;
import com.peakui.post.mapper.PostReportMapper;
import com.peakui.post.mapper.PostTagMapper;
import com.peakui.post.mapper.PostTagRelationMapper;
import com.peakui.post.model.dto.CreateCommentRequest;
import com.peakui.post.model.dto.ApplyAiSummaryCommentRequest;
import com.peakui.post.model.dto.CreatePostRequest;
import com.peakui.post.model.dto.CreateReportRequest;
import com.peakui.post.model.dto.PostImageRequest;
import com.peakui.post.model.dto.UpdatePostRequest;
import com.peakui.post.model.entity.Comment;
import com.peakui.post.model.entity.CommentLike;
import com.peakui.post.model.entity.Post;
import com.peakui.post.model.entity.PostCategory;
import com.peakui.post.model.entity.PostCollect;
import com.peakui.post.model.entity.PostContent;
import com.peakui.post.model.entity.PostEditHistory;
import com.peakui.post.model.entity.PostImage;
import com.peakui.post.model.entity.PostLike;
import com.peakui.post.model.entity.PostReport;
import com.peakui.post.model.entity.PostTag;
import com.peakui.post.model.entity.PostTagRelation;
import com.peakui.post.model.vo.CommentVO;
import com.peakui.post.model.vo.PostCategoryVO;
import com.peakui.post.model.vo.PostDetailVO;
import com.peakui.post.model.vo.PostImageVO;
import com.peakui.post.model.vo.PostListItemVO;
import com.peakui.post.model.vo.PostTagVO;
import com.peakui.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 帖子业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostContentMapper postContentMapper;
    private final PostImageMapper postImageMapper;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostCollectMapper postCollectMapper;
    private final PostTagMapper postTagMapper;
    private final PostTagRelationMapper postTagRelationMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final PostReportMapper postReportMapper;
    private final PostEditHistoryMapper postEditHistoryMapper;
    private final AuthFeignClient authFeignClient;
    private final MessageFeignClient messageFeignClient;
    private final HttpServletRequest request;
    private final LikeCacheService likeCacheService;
    private final LikeEventPublisher likeEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${post.ai.comment-user-id:900000000000000001}")
    private Long aiCommentUserId;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDetailVO createPost(CreatePostRequest request) {
        Long currentUserId = getCurrentUserId();
        PostCategory category = validateCategory(request.getCategoryId());
        LocalDateTime now = LocalDateTime.now();

        Post post = new Post();
        post.setUserId(currentUserId);
        post.setCategoryId(request.getCategoryId());
        post.setTitle(request.getTitle().trim());
        post.setSummary(normalizeText(request.getSummary()));
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setShareCount(0);
        post.setStatus(normalizeStatus(request.getStatus()));
        post.setIsTop(0);
        post.setIsEssence(0);
        post.setIsLock(0);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        if (Objects.equals(post.getStatus(), 1)) {
            post.setPublishedAt(now);
        }
        postMapper.insert(post);

        PostContent postContent = new PostContent();
        postContent.setPostId(post.getId());
        postContent.setContentType(request.getContentType());
        postContent.setContent(request.getContent());
        postContent.setWordCount(calculateWordCount(request.getContent()));
        postContent.setReadTime(calculateReadTime(postContent.getWordCount()));
        postContent.setVersion(1);
        postContent.setLastEditUserId(currentUserId);
        postContent.setLastEditTime(now);
        postContentMapper.insert(postContent);

        saveImages(post.getId(), request.getImages(), now);
        saveTags(post.getId(), request.getTagNames(), now);
        saveEditHistory(post.getId(), 1, currentUserId, request.getContent(), "创建帖子", 1, now);
        increaseCategoryPostCount(category);
        increaseActivityLevel(currentUserId);
        if (Objects.equals(post.getStatus(), 1)) {
            applicationEventPublisher.publishEvent(new PostPublishedEvent(post.getId(), post.getTitle(), request.getContent()));
        }

        log.info("创建帖子成功, postId={}, userId={}, status={}", post.getId(), currentUserId, post.getStatus());
        return buildPostDetail(post.getId(), currentUserId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDetailVO updatePost(Long postId, UpdatePostRequest request) {
        Long currentUserId = getCurrentUserId();
        Post post = getOwnedPost(postId, currentUserId);
        if (Objects.equals(post.getIsLock(), 1)) {
            throw new PostException("帖子已锁定，无法编辑");
        }

        PostCategory oldCategory = getCategoryById(post.getCategoryId());
        PostCategory newCategory = validateCategory(request.getCategoryId());
        PostContent oldContent = getPostContent(postId);
        boolean wasPublished = Objects.equals(post.getStatus(), 1);
        String oldTitle = post.getTitle();
        String oldBody = oldContent.getContent();
        LocalDateTime now = LocalDateTime.now();

        post.setCategoryId(request.getCategoryId());
        post.setTitle(request.getTitle().trim());
        post.setSummary(normalizeText(request.getSummary()));
        post.setStatus(normalizeStatus(request.getStatus()));
        post.setUpdatedAt(now);
        if (Objects.equals(post.getStatus(), 1) && post.getPublishedAt() == null) {
            post.setPublishedAt(now);
        }
        postMapper.updateById(post);

        PostContent postContent = getPostContent(postId);
        postContent.setContentType(request.getContentType());
        postContent.setContent(request.getContent());
        postContent.setWordCount(calculateWordCount(request.getContent()));
        postContent.setReadTime(calculateReadTime(postContent.getWordCount()));
        postContent.setVersion((postContent.getVersion() == null ? 1 : postContent.getVersion()) + 1);
        postContent.setLastEditUserId(currentUserId);
        postContent.setLastEditTime(now);
        postContentMapper.updateById(postContent);

        replaceImages(postId, request.getImages(), now);
        replaceTags(postId, request.getTagNames(), now);
        saveEditHistory(postId, postContent.getVersion(), currentUserId, request.getContent(), normalizeChangeDesc(request.getChangeDesc()), 2, now);
        adjustCategoryPostCount(oldCategory, newCategory);
        boolean published = Objects.equals(post.getStatus(), 1);
        boolean contentChanged = !Objects.equals(oldTitle, post.getTitle())
                || !Objects.equals(oldBody, request.getContent());
        if (published && (!wasPublished || contentChanged)) {
            applicationEventPublisher.publishEvent(new PostPublishedEvent(post.getId(), post.getTitle(), request.getContent()));
        }

        log.info("更新帖子成功, postId={}, userId={}, version={}", postId, currentUserId, postContent.getVersion());
        return buildPostDetail(postId, currentUserId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId) {
        Long currentUserId = getCurrentUserId();
        Post post = getOwnedPost(postId, currentUserId);
        if (Objects.equals(post.getStatus(), 3)) {
            return;
        }
        post.setStatus(3);
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.updateById(post);
        decreaseCategoryPostCount(getCategoryById(post.getCategoryId()));
        log.info("删除帖子成功, postId={}, userId={}", postId, currentUserId);
    }

    @Override
    public PostDetailVO getPostDetail(Long postId) {
        Long currentUserId = getCurrentUserIdNullable();
        getVisiblePost(postId, currentUserId);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
        return buildPostDetail(postId, currentUserId, true);
    }

    @Override
    public PageResponse<PostListItemVO> listPosts(Long categoryId, Integer status, String keyword, Long userId,
                                                  Long pageNum, Long pageSize, String sortBy) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .ne(Post::getStatus, 3);
        applyPostSort(wrapper, sortBy);
        if (categoryId != null) {
            wrapper.eq(Post::getCategoryId, categoryId);
        }
        if (status == null) {
            // Omitted status keeps the public default of published-only.
            wrapper.eq(Post::getStatus, 1);
        } else if (status >= 0) {
            wrapper.eq(Post::getStatus, status);
        }
        // status < 0 means "all" (admin view) — no status filter, only deleted excluded above.
        if (userId != null) {
            wrapper.eq(Post::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getSummary, keyword));
        }

        Page<Post> page = postMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        List<Post> posts = page.getRecords();
        if (posts.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }

        return buildPostPageResponse(posts, page.getTotal(), currentPage, currentSize);
    }

    @Override
    public void likePost(Long postId) {
        Long currentUserId = getCurrentUserId();
        ensurePostLikeable(postId, currentUserId);
        LikeMutationResult mutation = likeCacheService.mutate(postId, currentUserId, true);
        if (!mutation.changed()) {
            return;
        }
        LikeEvent event = new LikeEvent(mutation.eventId(), postId, currentUserId, true,
                mutation.version(), System.currentTimeMillis());
        likeEventPublisher.publish(event,
                () -> likeCacheService.rollback(postId, currentUserId, true, mutation.version()));
        log.info("点赞帖子成功, postId={}, userId={}", postId, currentUserId);
    }

    @Override
    public void unlikePost(Long postId) {
        Long currentUserId = getCurrentUserId();
        ensurePostLikeable(postId, currentUserId);
        LikeMutationResult mutation = likeCacheService.mutate(postId, currentUserId, false);
        if (!mutation.changed()) {
            return;
        }
        LikeEvent event = new LikeEvent(mutation.eventId(), postId, currentUserId, false,
                mutation.version(), System.currentTimeMillis());
        likeEventPublisher.publish(event,
                () -> likeCacheService.rollback(postId, currentUserId, false, mutation.version()));
        log.info("取消点赞帖子成功, postId={}, userId={}", postId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectPost(Long postId, Long collectFolderId) {
        Long currentUserId = getCurrentUserId();
        ensurePostLikeable(postId, currentUserId);
        long count = postCollectMapper.selectCount(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, currentUserId));
        if (count > 0) {
            return;
        }

        PostCollect postCollect = new PostCollect();
        postCollect.setPostId(postId);
        postCollect.setUserId(currentUserId);
        postCollect.setCollectFolderId(collectFolderId);
        postCollect.setCreatedAt(LocalDateTime.now());
        postCollectMapper.insert(postCollect);

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("collect_count = IFNULL(collect_count, 0) + 1"));
        log.info("收藏帖子成功, postId={}, userId={}, folderId={}", postId, currentUserId, collectFolderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectPost(Long postId) {
        Long currentUserId = getCurrentUserId();
        PostCollect postCollect = postCollectMapper.selectOne(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, currentUserId)
                .last("limit 1"));
        if (postCollect == null) {
            return;
        }
        postCollectMapper.deleteById(postCollect.getId());
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("collect_count = GREATEST(IFNULL(collect_count, 0) - 1, 0)"));
        log.info("取消收藏帖子成功, postId={}, userId={}", postId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sharePost(Long postId) {
        getVisiblePost(postId, null);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("share_count = IFNULL(share_count, 0) + 1"));
        log.info("分享帖子成功, postId={}", postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long postId, CreateCommentRequest request) {
        Long currentUserId = getCurrentUserId();
        Post post = getVisiblePost(postId, currentUserId);
        if (Objects.equals(post.getIsLock(), 1)) {
            throw new PostException("帖子已锁定，暂不允许评论");
        }

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentMapper.selectById(request.getParentId());
            if (parentComment == null || !Objects.equals(parentComment.getPostId(), postId)) {
                throw new PostException("父评论不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(currentUserId);
        comment.setParentId(request.getParentId());
        comment.setReplyToUserId(parentComment == null ? null : parentComment.getUserId());
        comment.setContent(request.getContent().trim());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setIpAddress(limitLength(getClientIp(), 45));
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);

        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = IFNULL(comment_count, 0) + 1"));

        if (parentComment != null) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, parentComment.getId())
                    .setSql("reply_count = IFNULL(reply_count, 0) + 1"));
        }

        notifyCommentReply(post, comment, parentComment, currentUserId);
        log.info("创建帖子评论成功, postId={}, commentId={}, userId={}, parentId={}",
                postId, comment.getId(), currentUserId, request.getParentId());
        return toCommentVO(commentMapper.selectById(comment.getId()));
    }

    private void notifyCommentReply(Post post, Comment comment, Comment parentComment, Long currentUserId) {
        try {
            Set<Long> recipients = new LinkedHashSet<>();
            if (post.getUserId() != null && !Objects.equals(post.getUserId(), currentUserId)) {
                recipients.add(post.getUserId());
            }
            if (parentComment != null && parentComment.getUserId() != null
                    && !Objects.equals(parentComment.getUserId(), currentUserId)) {
                recipients.add(parentComment.getUserId());
            }
            if (recipients.isEmpty()) {
                return;
            }
            AuthFeignClient.UserProfileResponse profile = fetchUserProfile(currentUserId);
            String fromNickname = resolveUserName(profile, currentUserId);
            String fromAvatar = resolveUserAvatar(profile);
            String preview = limitLength(comment.getContent(), 200);
            List<CommentReplyNotifyRequest> requests = recipients.stream()
                    .map(toUserId -> {
                        CommentReplyNotifyRequest notify = new CommentReplyNotifyRequest();
                        notify.setSourceType("post");
                        notify.setSourceId(post.getId());
                        notify.setSourceTitle(post.getTitle());
                        notify.setCommentId(comment.getId());
                        notify.setParentCommentId(comment.getParentId());
                        notify.setFromUserId(currentUserId);
                        notify.setFromNickname(fromNickname);
                        notify.setFromAvatar(fromAvatar);
                        notify.setToUserId(toUserId);
                        notify.setContentPreview(preview);
                        return notify;
                    })
                    .toList();
            sendCommentReplyNotifications(requests);
        } catch (Exception e) {
            log.warn("构建评论回复通知失败, postId={}, commentId={}", post.getId(), comment.getId(), e);
        }
    }

    private void sendCommentReplyNotifications(List<CommentReplyNotifyRequest> requests) {
        Runnable task = () -> requests.forEach(notify -> {
            try {
                messageFeignClient.notifyCommentReply(notify);
            } catch (Exception e) {
                log.warn("发送评论回复通知失败, toUserId={}", notify.getToUserId(), e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Override
    public List<CommentVO> listComments(Long postId) {
        Long currentUserId = getCurrentUserIdNullable();
        getVisiblePost(postId, currentUserId);
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreatedAt));
        return buildCommentTree(comments, currentUserId, loadLikedCommentIds(comments, currentUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long postId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(postId, commentId);
        long existing = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, currentUserId));
        if (existing > 0) {
            return;
        }
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(currentUserId);
        like.setCreatedAt(LocalDateTime.now());
        commentLikeMapper.insert(like);
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .setSql("like_count = IFNULL(like_count, 0) + 1"));
        log.info("点赞文章评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long postId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(postId, commentId);
        CommentLike like = commentLikeMapper.selectOne(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, currentUserId)
                .last("limit 1"));
        if (like == null) {
            return;
        }
        commentLikeMapper.deleteById(like.getId());
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        log.info("取消点赞文章评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    private Comment requireVisibleComment(Long postId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null
                || !Objects.equals(comment.getPostId(), postId)
                || Objects.equals(comment.getStatus(), 3)) {
            throw new PostException("评论不存在");
        }
        return comment;
    }

    private Set<Long> loadLikedCommentIds(List<Comment> comments, Long currentUserId) {
        if (currentUserId == null || comments == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).filter(Objects::nonNull).toList();
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .eq(CommentLike::getUserId, currentUserId)
                        .in(CommentLike::getCommentId, commentIds))
                .stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long postId, Long commentId) {
        Long currentUserId = getCurrentUserId();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getPostId(), postId) || Objects.equals(comment.getStatus(), 3)) {
            throw new PostException("评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), currentUserId) && !isAdmin()) {
            Post post = getOwnedPost(postId, currentUserId);
            if (!Objects.equals(post.getUserId(), currentUserId)) {
                throw new PostException("无权删除该评论");
            }
        }

        int removedCount = markCommentDeleted(commentId);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = GREATEST(IFNULL(comment_count, 0) - " + removedCount + ", 0)"));

        if (comment.getParentId() != null) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, comment.getParentId())
                    .setSql("reply_count = GREATEST(IFNULL(reply_count, 0) - 1, 0)"));
        }
        log.info("删除帖子评论成功, postId={}, commentId={}, userId={}, removedCount={}",
                postId, commentId, currentUserId, removedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportPost(Long postId, CreateReportRequest request) {
        Long currentUserId = getCurrentUserId();
        getVisiblePost(postId, currentUserId);
        if (request.getCommentId() != null) {
            Comment comment = commentMapper.selectById(request.getCommentId());
            if (comment == null || !Objects.equals(comment.getPostId(), postId)) {
                throw new PostException("被举报评论不存在");
            }
        }

        PostReport postReport = new PostReport();
        postReport.setPostId(postId);
        postReport.setCommentId(request.getCommentId());
        postReport.setReporterId(currentUserId);
        postReport.setReportType(request.getReportType());
        postReport.setReportReason(request.getReportReason().trim());
        postReport.setReportStatus(0);
        postReport.setCreatedAt(LocalDateTime.now());
        postReportMapper.insert(postReport);
        log.info("举报帖子成功, postId={}, reportId={}, reporterId={}, type={}",
                postId, postReport.getId(), currentUserId, request.getReportType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAiSummaryAndComment(Long postId, ApplyAiSummaryCommentRequest request) {
        Post post = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                .eq(Post::getId, postId).last("FOR UPDATE"));
        if (post == null || !Objects.equals(post.getStatus(), 1) || Objects.equals(post.getIsLock(), 1)) {
            return;
        }
        if (!Objects.equals(request.getSourceHash(),
                PostContentFingerprint.of(post.getTitle(), getPostContent(postId).getContent()))) {
            return; // An edit overtook this generation; discard stale content.
        }
        ApiResponse<AuthFeignClient.UserProfileResponse> account = authFeignClient.getUserProfile(aiCommentUserId);
        if (account == null || !Objects.equals(account.getCode(), 0) || account.getData() == null
                || !Objects.equals(account.getData().getUsername(), "matu_ai")
                || !Objects.equals(account.getData().getStatus(), 1) || account.getData().getDeletedAt() != null) {
            throw new PostException("AI账号不存在、身份不匹配或已禁用");
        }
        String commentText = "📌 文章摘要（AI 自动生成）：\n" + request.getSummary().trim();
        Comment existing = commentMapper.selectOne(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getUserId, aiCommentUserId)
                .isNull(Comment::getParentId)
                .likeRight(Comment::getContent, "📌 文章摘要（AI 自动生成）：")
                .last("limit 1"));
        if (existing != null) {
            // Respect moderator deletion; update the same visible AI comment after article edits.
            if (Objects.equals(existing.getStatus(), 1) && !Objects.equals(existing.getContent(), commentText)) {
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, existing.getId()).eq(Comment::getStatus, 1)
                        .set(Comment::getContent, commentText).set(Comment::getUpdatedAt, LocalDateTime.now()));
                log.info("更新AI摘要评论成功, postId={}, commentId={}", postId, existing.getId());
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(aiCommentUserId);
        comment.setContent(commentText);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = IFNULL(comment_count, 0) + 1"));
        log.info("创建AI摘要评论成功, postId={}, commentId={}, aiUserId={}", postId, comment.getId(), aiCommentUserId);
    }

    @Override
    public List<PostCategoryVO> listCategories() {
        return postCategoryMapper.selectList(new LambdaQueryWrapper<PostCategory>()
                        .eq(PostCategory::getStatus, 1)
                        .orderByAsc(PostCategory::getSortOrder)
                        .orderByAsc(PostCategory::getId))
                .stream()
                .map(category -> PostCategoryVO.builder()
                        .id(category.getId())
                        .parentId(category.getParentId())
                        .categoryName(category.getCategoryName())
                        .categoryDesc(category.getCategoryDesc())
                        .iconUrl(category.getIconUrl())
                        .sortOrder(category.getSortOrder())
                        .postCount(category.getPostCount())
                        .status(category.getStatus())
                        .build())
                .toList();
    }

    @Override
    public PageResponse<PostListItemVO> listDrafts(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<Post> page = postMapper.selectPage(new Page<>(currentPage, currentSize), new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, currentUserId)
                .eq(Post::getStatus, 0)
                .ne(Post::getStatus, 3)
                .orderByDesc(Post::getUpdatedAt));
        return buildPostPageResponse(page.getRecords(), page.getTotal(), currentPage, currentSize);
    }

    @Override
    public PageResponse<PostListItemVO> listCollectedPosts(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<PostCollect> page = postCollectMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<PostCollect>()
                        .eq(PostCollect::getUserId, currentUserId)
                        .orderByDesc(PostCollect::getCreatedAt));
        List<Long> postIds = page.getRecords().stream().map(PostCollect::getPostId).distinct().toList();
        if (postIds.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        Map<Long, Post> postMap = postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .in(Post::getId, postIds)
                        .ne(Post::getStatus, 3))
                .stream().collect(Collectors.toMap(Post::getId, post -> post));
        List<Post> orderedCollected = postIds.stream().map(postMap::get).filter(Objects::nonNull).toList();
        return buildPostPageResponse(orderedCollected, page.getTotal(), currentPage, currentSize);
    }

    @Override
    public PageResponse<PostListItemVO> listLikedPosts(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<PostLike> page = postLikeMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, currentUserId)
                        .orderByDesc(PostLike::getCreatedAt));
        List<Long> postIds = page.getRecords().stream().map(PostLike::getPostId).distinct().toList();
        if (postIds.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        Map<Long, Post> postMap = postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .in(Post::getId, postIds)
                        .ne(Post::getStatus, 3))
                .stream().collect(Collectors.toMap(Post::getId, post -> post));
        List<Post> orderedLiked = postIds.stream().map(postMap::get).filter(Objects::nonNull).toList();
        return buildPostPageResponse(orderedLiked, page.getTotal(), currentPage, currentSize);
    }

    @Override
    public List<PostTagVO> listTags(String keyword) {
        LambdaQueryWrapper<PostTag> wrapper = new LambdaQueryWrapper<PostTag>()
                .eq(PostTag::getStatus, 1)
                .orderByDesc(PostTag::getPostCount)
                .orderByAsc(PostTag::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PostTag::getTagName, keyword.trim());
        }
        return postTagMapper.selectList(wrapper)
                .stream()
                .map(tag -> PostTagVO.builder()
                        .id(tag.getId())
                        .tagName(tag.getTagName())
                        .tagDesc(tag.getTagDesc())
                        .postCount(tag.getPostCount())
                        .status(tag.getStatus())
                        .build())
                .toList();
    }

    private Post getOwnedPost(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Objects.equals(post.getStatus(), 3)) {
            throw new PostException("帖子不存在");
        }
        if (!Objects.equals(post.getUserId(), currentUserId) && !isAdmin()) {
            throw new PostException("无权操作该帖子");
        }
        return post;
    }

    private boolean isAdmin() {
        String roles = request.getHeader("X-User-Roles");
        if (hasAdminRole(parseRoles(roles))) {
            return true;
        }

        Long currentUserId = getCurrentUserIdNullable();
        if (Objects.equals(currentUserId, 1L)) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }

        try {
            ApiResponse<AuthFeignClient.UserRolesResponse> response = authFeignClient.getUserRoles(currentUserId);
            AuthFeignClient.UserRolesResponse data = response == null ? null : response.getData();
            return data != null && hasAdminRole(data.getRoles());
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Collections.emptyList();
        }
        return List.of(roles.split(","));
    }

    private boolean hasAdminRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(role -> "ADMIN".equals(role)
                        || "ROLE_ADMIN".equals(role)
                        || "SUPER_ADMIN".equals(role)
                        || "MANAGER".equals(role));
    }

    private Post getVisiblePost(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Objects.equals(post.getStatus(), 3)) {
            throw new PostException("帖子不存在");
        }
        if (!Objects.equals(post.getStatus(), 1)
                && (currentUserId == null || !Objects.equals(post.getUserId(), currentUserId))) {
            throw new PostException("帖子暂不可见");
        }
        return post;
    }

    private void ensurePostLikeable(Long postId, Long currentUserId) {
        Post post = getVisiblePost(postId, currentUserId);
        if (Objects.equals(post.getIsLock(), 1)) {
            throw new PostException("帖子已锁定，暂不可操作");
        }
    }

    private PostContent getPostContent(Long postId) {
        PostContent postContent = postContentMapper.selectOne(new LambdaQueryWrapper<PostContent>()
                .eq(PostContent::getPostId, postId)
                .last("limit 1"));
        if (postContent == null) {
            throw new PostException("帖子内容不存在");
        }
        return postContent;
    }

    private PostCategory validateCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        PostCategory category = postCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new PostException("分类不存在或已禁用");
        }
        return category;
    }

    private PostCategory getCategoryById(Long categoryId) {
        return categoryId == null ? null : postCategoryMapper.selectById(categoryId);
    }

    private void saveImages(Long postId, List<PostImageRequest> images, LocalDateTime now) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (PostImageRequest imageRequest : images) {
            PostImage postImage = new PostImage();
            postImage.setPostId(postId);
            postImage.setImageUrl(imageRequest.getImageUrl().trim());
            postImage.setImageType(normalizeImageType(imageRequest.getImageType()));
            postImage.setSortOrder(defaultNumber(imageRequest.getSortOrder()));
            postImage.setCreatedAt(now);
            postImageMapper.insert(postImage);
        }
    }

    private void replaceImages(Long postId, List<PostImageRequest> images, LocalDateTime now) {
        postImageMapper.delete(new LambdaQueryWrapper<PostImage>().eq(PostImage::getPostId, postId));
        saveImages(postId, images, now);
    }

    private void saveTags(Long postId, List<String> tagNames, LocalDateTime now) {
        Set<String> normalizedTags = normalizeTagNames(tagNames);
        if (normalizedTags.isEmpty()) {
            return;
        }
        for (String tagName : normalizedTags) {
            PostTag tag = postTagMapper.selectOne(new LambdaQueryWrapper<PostTag>()
                    .eq(PostTag::getTagName, tagName)
                    .last("limit 1"));
            if (tag == null) {
                tag = new PostTag();
                tag.setTagName(tagName);
                tag.setPostCount(0);
                tag.setStatus(1);
                tag.setCreatedAt(now);
                postTagMapper.insert(tag);
            }
            PostTagRelation relation = new PostTagRelation();
            relation.setPostId(postId);
            relation.setTagId(tag.getId());
            relation.setCreatedAt(now);
            postTagRelationMapper.insert(relation);
            postTagMapper.update(null, new LambdaUpdateWrapper<PostTag>()
                    .eq(PostTag::getId, tag.getId())
                    .setSql("post_count = IFNULL(post_count, 0) + 1"));
        }
    }

    private void replaceTags(Long postId, List<String> tagNames, LocalDateTime now) {
        List<PostTagRelation> oldRelations = postTagRelationMapper.selectList(new LambdaQueryWrapper<PostTagRelation>()
                .eq(PostTagRelation::getPostId, postId));
        for (PostTagRelation relation : oldRelations) {
            postTagMapper.update(null, new LambdaUpdateWrapper<PostTag>()
                    .eq(PostTag::getId, relation.getTagId())
                    .setSql("post_count = GREATEST(IFNULL(post_count, 0) - 1, 0)"));
        }
        postTagRelationMapper.delete(new LambdaQueryWrapper<PostTagRelation>().eq(PostTagRelation::getPostId, postId));
        saveTags(postId, tagNames, now);
    }

    private void saveEditHistory(Long postId,
                                 Integer version,
                                 Long editorId,
                                 String content,
                                 String changeDesc,
                                 Integer changeType,
                                 LocalDateTime now) {
        PostEditHistory history = new PostEditHistory();
        history.setPostId(postId);
        history.setVersion(version);
        history.setEditorId(editorId);
        history.setEditorName(String.valueOf(editorId));
        history.setContentSnapshot(content);
        history.setChangeDesc(changeDesc);
        history.setChangeType(changeType);
        history.setCreatedAt(now);
        postEditHistoryMapper.insert(history);
    }

    private void increaseActivityLevel(Long userId) {
        ApiResponse<Void> response = authFeignClient.increaseActivityLevel(userId);
        if (response == null || response.getCode() == null || response.getCode() != 0) {
            throw new PostException(response == null ? "更新用户活跃度失败" : response.getMessage());
        }
    }

    private void increaseCategoryPostCount(PostCategory category) {
        if (category == null) {
            return;
        }
        postCategoryMapper.update(null, new LambdaUpdateWrapper<PostCategory>()
                .eq(PostCategory::getId, category.getId())
                .setSql("post_count = IFNULL(post_count, 0) + 1"));
    }

    private void decreaseCategoryPostCount(PostCategory category) {
        if (category == null) {
            return;
        }
        postCategoryMapper.update(null, new LambdaUpdateWrapper<PostCategory>()
                .eq(PostCategory::getId, category.getId())
                .setSql("post_count = GREATEST(IFNULL(post_count, 0) - 1, 0)"));
    }

    private void adjustCategoryPostCount(PostCategory oldCategory, PostCategory newCategory) {
        Long oldCategoryId = oldCategory == null ? null : oldCategory.getId();
        Long newCategoryId = newCategory == null ? null : newCategory.getId();
        if (Objects.equals(oldCategoryId, newCategoryId)) {
            return;
        }
        decreaseCategoryPostCount(oldCategory);
        increaseCategoryPostCount(newCategory);
    }

    private PostDetailVO buildPostDetail(Long postId, Long currentUserId, boolean reloadPostAfterView) {
        Post post = postMapper.selectById(postId);
        if (reloadPostAfterView) {
            post = postMapper.selectById(postId);
        }
        PostContent postContent = getPostContent(postId);
        List<PostImage> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
                .eq(PostImage::getPostId, postId)
                .orderByAsc(PostImage::getSortOrder)
                .orderByAsc(PostImage::getId));
        List<PostTagRelation> relations = postTagRelationMapper.selectList(new LambdaQueryWrapper<PostTagRelation>()
                .eq(PostTagRelation::getPostId, postId));

        List<String> tags = loadTagNamesByRelations(relations);
        PostCategory category = getCategoryById(post.getCategoryId());
        List<CommentVO> recentComments = loadRecentComments(postId, currentUserId, 3);

        AuthFeignClient.UserProfileResponse userProfile = fetchUserProfile(post.getUserId());

        return PostDetailVO.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .authorName(resolveUserName(userProfile, post.getUserId()))
                .authorAvatar(resolveUserAvatar(userProfile))
                .authorSchoolName(resolveSchoolName(userProfile))
                .authorCompanyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .owner(isOwner(post.getUserId(), currentUserId))
                .categoryId(post.getCategoryId())
                .categoryName(category == null ? null : category.getCategoryName())
                .title(post.getTitle())
                .summary(post.getSummary())
                .contentType(postContent.getContentType())
                .content(postContent.getContent())
                .wordCount(defaultNumber(postContent.getWordCount()))
                .readTime(defaultNumber(postContent.getReadTime()))
                .version(defaultNumber(postContent.getVersion()))
                .viewCount(defaultNumber(post.getViewCount()))
                .likeCount(likeCacheService.getLikeCount(post.getId()))
                .commentCount(defaultNumber(post.getCommentCount()))
                .collectCount(defaultNumber(post.getCollectCount()))
                .shareCount(defaultNumber(post.getShareCount()))
                .status(defaultNumber(post.getStatus()))
                .isTop(defaultNumber(post.getIsTop()))
                .isEssence(defaultNumber(post.getIsEssence()))
                .isLock(defaultNumber(post.getIsLock()))
                .lockReason(post.getLockReason())
                .liked(hasLiked(postId, currentUserId))
                .collected(hasCollected(postId, currentUserId))
                .topCommentCount(recentComments.size())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .publishedAt(post.getPublishedAt())
                .tags(tags)
                .images(images.stream().map(this::toImageVO).toList())
                .recentComments(recentComments)
                .build();
    }

    private Map<Long, PostCategory> loadCategoryMap(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return postCategoryMapper.selectBatchIds(categoryIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PostCategory::getId, Function.identity(), (a, b) -> a));
    }

    private PageResponse<PostListItemVO> buildPostPageResponse(List<Post> posts, long total, long currentPage, long currentSize) {
        if (posts == null || posts.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, total, Collections.emptyList());
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, PostCategory> categoryMap = loadCategoryMap(posts.stream().map(Post::getCategoryId).filter(Objects::nonNull).toList());
        Map<Long, List<String>> tagMap = loadTagMap(postIds);
        Map<Long, String> coverMap = loadCoverMap(postIds);
        Map<Long, String> contentMap = loadPostContentMap(postIds);
        Map<Long, AuthFeignClient.UserProfileResponse> userMap = loadUserProfileMap(posts.stream().map(Post::getUserId).collect(Collectors.toSet()));
        Long currentUserId = getCurrentUserIdNullable();

        List<PostListItemVO> records = posts.stream()
                .map(post -> PostListItemVO.builder()
                        .id(post.getId())
                        .userId(post.getUserId())
                        .authorName(resolveUserName(userMap.get(post.getUserId()), post.getUserId()))
                        .authorAvatar(resolveUserAvatar(userMap.get(post.getUserId())))
                        .authorSchoolName(resolveSchoolName(userMap.get(post.getUserId())))
                        .authorCompanyName(resolveCompanyName(userMap.get(post.getUserId())))
                        .authorTitle(resolveTitle(userMap.get(post.getUserId())))
                        .authorIsVip(resolveAuthorIsVip(userMap.get(post.getUserId())))
                        .owner(isOwner(post.getUserId(), currentUserId))
                        .categoryId(post.getCategoryId())
                        .categoryName(categoryMap.containsKey(post.getCategoryId()) ? categoryMap.get(post.getCategoryId()).getCategoryName() : null)
                        .title(post.getTitle())
                        .summary(resolveListSummary(post, contentMap.get(post.getId())))
                        .viewCount(defaultNumber(post.getViewCount()))
                        .likeCount(likeCacheService.getLikeCount(post.getId()))
                        .commentCount(defaultNumber(post.getCommentCount()))
                        .collectCount(defaultNumber(post.getCollectCount()))
                        .shareCount(defaultNumber(post.getShareCount()))
                        .status(post.getStatus())
                        .isTop(defaultNumber(post.getIsTop()))
                        .isEssence(defaultNumber(post.getIsEssence()))
                        .createdAt(post.getCreatedAt())
                        .publishedAt(post.getPublishedAt())
                        .tags(tagMap.getOrDefault(post.getId(), Collections.emptyList()))
                        .coverImage(resolveCoverImage(coverMap.get(post.getId()), contentMap.get(post.getId())))
                        .build())
                .toList();
        return PageResponse.of(currentPage, currentSize, total, records);
    }

    private Map<Long, List<String>> loadTagMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostTagRelation> relations = postTagRelationMapper.selectList(new LambdaQueryWrapper<PostTagRelation>()
                .in(PostTagRelation::getPostId, postIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = relations.stream().map(PostTagRelation::getTagId).distinct().toList();
        Map<Long, PostTag> tagMap = postTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(PostTag::getId, Function.identity(), (a, b) -> a));

        Map<Long, List<String>> result = relations.stream()
                .collect(Collectors.groupingBy(PostTagRelation::getPostId,
                        Collectors.mapping(relation -> {
                            PostTag tag = tagMap.get(relation.getTagId());
                            return tag == null ? null : tag.getTagName();
                        }, Collectors.toList())));
        result.replaceAll((key, value) -> value.stream().filter(StringUtils::hasText).distinct().toList());
        return result;
    }

    private Map<Long, String> loadCoverMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostImage> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
                .in(PostImage::getPostId, postIds)
                .orderByAsc(PostImage::getSortOrder)
                .orderByAsc(PostImage::getId));
        Map<Long, String> result = images.stream()
                .filter(image -> StringUtils.hasText(image.getImageUrl()))
                .collect(Collectors.toMap(PostImage::getPostId, PostImage::getImageUrl, (first, second) -> first));

        for (PostImage image : images) {
            if (Objects.equals("灏侀潰", image.getImageType())) {
                result.put(image.getPostId(), image.getImageUrl());
            }
        }
        return result;
    }

    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\(\\s*([^)\\s]+?)(?:\\s+[\"'][^\"']*[\"'])?\\s*\\)");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern MARKDOWN_URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_MARKER = Pattern.compile("[#>*_`~!\\-\\[\\]()]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int LIST_SUMMARY_MAX_LENGTH = 200;

    private Map<Long, String> loadPostContentMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return postContentMapper.selectList(new LambdaQueryWrapper<PostContent>()
                        .in(PostContent::getPostId, postIds))
                .stream()
                .filter(content -> content != null && content.getPostId() != null && StringUtils.hasText(content.getContent()))
                .collect(Collectors.toMap(PostContent::getPostId, PostContent::getContent, (first, second) -> first));
    }

    private String resolveListSummary(Post post, String content) {
        // Prefer an excerpt of the real body; the stored summary may be stale or,
        // for older rows, polluted with an image's alt text and URL.
        if (StringUtils.hasText(content)) {
            return buildContentExcerpt(content);
        }
        return post.getSummary();
    }

    private String resolveCoverImage(String storedCover, String content) {
        if (StringUtils.hasText(storedCover)) {
            return storedCover;
        }
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Matcher imageMatcher = MARKDOWN_IMAGE.matcher(content);
        return imageMatcher.find() ? imageMatcher.group(1) : null;
    }

    private String buildContentExcerpt(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return null;
        }
        String text = MARKDOWN_CODE_BLOCK.matcher(markdown).replaceAll(" ");
        text = MARKDOWN_IMAGE.matcher(text).replaceAll(" ");
        text = MARKDOWN_LINK.matcher(text).replaceAll("$1");
        text = MARKDOWN_URL.matcher(text).replaceAll(" ");
        text = MARKDOWN_MARKER.matcher(text).replaceAll(" ");
        text = WHITESPACE.matcher(text).replaceAll(" ").trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.length() <= LIST_SUMMARY_MAX_LENGTH ? text : text.substring(0, LIST_SUMMARY_MAX_LENGTH);
    }

    private List<String> loadTagNamesByRelations(List<PostTagRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = relations.stream().map(PostTagRelation::getTagId).toList();
        Map<Long, PostTag> tagMap = postTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(PostTag::getId, Function.identity(), (a, b) -> a));
        List<String> tags = new ArrayList<>();
        for (PostTagRelation relation : relations) {
            PostTag tag = tagMap.get(relation.getTagId());
            if (tag != null && StringUtils.hasText(tag.getTagName())) {
                tags.add(tag.getTagName());
            }
        }
        return tags.stream().distinct().toList();
    }

    private List<CommentVO> loadRecentComments(Long postId, Long currentUserId, int limit) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 1)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt)
                .last("limit " + Math.max(limit, 0)));
        Set<Long> likedCommentIds = loadLikedCommentIds(comments, currentUserId);
        return comments.stream()
                .map(comment -> toCommentVO(comment, currentUserId, Collections.emptyList(), likedCommentIds))
                .toList();
    }

    private List<CommentVO> buildCommentTree(List<Comment> comments, Long currentUserId, Set<Long> likedCommentIds) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<Comment>> childrenMap = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        return comments.stream()
                .filter(comment -> comment.getParentId() == null)
                .map(comment -> toCommentVO(comment, currentUserId, buildReplyList(comment.getId(), childrenMap, currentUserId, likedCommentIds), likedCommentIds))
                .toList();
    }

    private List<CommentVO> buildReplyList(Long parentId, Map<Long, List<Comment>> childrenMap, Long currentUserId, Set<Long> likedCommentIds) {
        List<Comment> replies = childrenMap.getOrDefault(parentId, Collections.emptyList());
        return replies.stream()
                .map(reply -> toCommentVO(reply, currentUserId, buildReplyList(reply.getId(), childrenMap, currentUserId, likedCommentIds), likedCommentIds))
                .toList();
    }

    private CommentVO toCommentVO(Comment comment, Long currentUserId, List<CommentVO> replies, Set<Long> likedCommentIds) {
        AuthFeignClient.UserProfileResponse userProfile = fetchUserProfile(comment.getUserId());
        return CommentVO.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .replyToUserId(comment.getReplyToUserId())
                .content(comment.getContent())
                .likeCount(defaultNumber(comment.getLikeCount()))
                .liked(likedCommentIds.contains(comment.getId()))
                .replyCount(defaultNumber(comment.getReplyCount()))
                .status(defaultNumber(comment.getStatus()))
                .ipAddress(comment.getIpAddress())
                .userName(resolveUserName(userProfile, comment.getUserId()))
                .userAvatar(resolveUserAvatar(userProfile))
                .userSchoolName(resolveSchoolName(userProfile))
                .userCompanyName(resolveCompanyName(userProfile))
                .userTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .owner(isOwner(comment.getUserId(), currentUserId))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }

    private CommentVO toCommentVO(Comment comment) {
        return toCommentVO(comment, getCurrentUserIdNullable(), Collections.emptyList(), Collections.emptySet());
    }

    private PostImageVO toImageVO(PostImage image) {
        return PostImageVO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType())
                .sortOrder(defaultNumber(image.getSortOrder()))
                .build();
    }

    private Set<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (StringUtils.hasText(tagName)) {
                result.add(tagName.trim());
            }
        }
        return result;
    }

    private Integer calculateWordCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return content.trim().length();
    }

    private Integer calculateReadTime(Integer wordCount) {
        if (wordCount == null || wordCount <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(wordCount / 300.0));
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeImageType(String imageType) {
        return StringUtils.hasText(imageType) ? imageType.trim() : "鍐呭";
    }

    private String normalizeChangeDesc(String changeDesc) {
        return StringUtils.hasText(changeDesc) ? changeDesc.trim() : "缂栬緫甯栧瓙";
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status < 0 || status > 3) {
            throw new PostException("甯栧瓙鐘舵€佷笉鍚堟硶");
        }
        return status;
    }

    private void applyPostSort(LambdaQueryWrapper<Post> wrapper, String sortBy) {
        String normalizedSort = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase() : "latest";
        wrapper.orderByDesc(Post::getIsTop);
        switch (normalizedSort) {
            case "hot" -> wrapper.orderByDesc(Post::getCommentCount)
                    .orderByDesc(Post::getLikeCount)
                    .orderByDesc(Post::getViewCount)
                    .orderByDesc(Post::getCreatedAt);
            case "like" -> wrapper.orderByDesc(Post::getLikeCount)
                    .orderByDesc(Post::getCreatedAt);
            case "view" -> wrapper.orderByDesc(Post::getViewCount)
                    .orderByDesc(Post::getCreatedAt);
            default -> wrapper.orderByDesc(Post::getCreatedAt);
        }
    }

    private int markCommentDeleted(Long commentId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getId, commentId)
                .or()
                .eq(Comment::getParentId, commentId));
        if (comments.isEmpty()) {
            return 0;
        }
        List<Long> ids = comments.stream()
                .filter(comment -> !Objects.equals(comment.getStatus(), 3))
                .map(Comment::getId)
                .toList();
        if (ids.isEmpty()) {
            return 0;
        }
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .in(Comment::getId, ids)
                .set(Comment::getStatus, 3)
                .set(Comment::getUpdatedAt, LocalDateTime.now()));
        return ids.size();
    }

    private boolean hasLiked(Long postId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return likeCacheService.hasLiked(postId, currentUserId);
    }

    private boolean hasCollected(Long postId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return postCollectMapper.selectCount(new LambdaQueryWrapper<PostCollect>()
                .eq(PostCollect::getPostId, postId)
                .eq(PostCollect::getUserId, currentUserId)) > 0;
    }

    private Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean shouldSkipViewCount() {
        return "development".equalsIgnoreCase(request.getHeader("X-Client-Env"));
    }

    private boolean isOwner(Long resourceUserId, Long currentUserId) {
        return currentUserId != null && Objects.equals(resourceUserId, currentUserId);
    }

    private Map<Long, AuthFeignClient.UserProfileResponse> loadUserProfileMap(Set<Long> userIds) {
        Map<Long, AuthFeignClient.UserProfileResponse> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        for (Long userId : userIds) {
            AuthFeignClient.UserProfileResponse userProfile = fetchUserProfile(userId);
            if (userProfile != null) {
                result.put(userId, userProfile);
            }
        }
        return result;
    }

    private AuthFeignClient.UserProfileResponse fetchUserProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            ApiResponse<AuthFeignClient.UserProfileResponse> response = authFeignClient.getUserProfile(userId);
            return response == null ? null : response.getData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUserName(AuthFeignClient.UserProfileResponse userProfile, Long userId) {
        if (userProfile != null && StringUtils.hasText(userProfile.getNickname())) {
            return userProfile.getNickname();
        }
        if (userProfile != null && StringUtils.hasText(userProfile.getUsername())) {
            return userProfile.getUsername();
        }
        return userId == null ? null : "用户" + userId;
    }

    private String resolveUserAvatar(AuthFeignClient.UserProfileResponse userProfile) {
        return userProfile == null ? null : userProfile.getAvatarUrl();
    }

    private String resolveSchoolName(AuthFeignClient.UserProfileResponse userProfile) {
        return userProfile == null ? null : userProfile.getSchoolName();
    }

    private String resolveCompanyName(AuthFeignClient.UserProfileResponse userProfile) {
        return userProfile == null ? null : userProfile.getCompanyName();
    }

    private String resolveTitle(AuthFeignClient.UserProfileResponse userProfile) {
        return userProfile == null ? null : userProfile.getTitle();
    }

    private Integer resolveAuthorIsVip(AuthFeignClient.UserProfileResponse userProfile) {
        if (userProfile == null || !Objects.equals(userProfile.getIsVip(), 1)
                || userProfile.getVipExpiredAt() == null) {
            return 0;
        }
        return userProfile.getVipExpiredAt().isAfter(LocalDateTime.now()) ? 1 : 0;
    }

    private Long getCurrentUserId() {
        String userId = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new PostException("未登录或登录已失效");
        }
        return Long.parseLong(userId);
    }

    private Long getCurrentUserIdNullable() {
        String userId = request.getHeader("X-User-Id");
        return StringUtils.hasText(userId) ? Long.parseLong(userId) : null;
    }


    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
