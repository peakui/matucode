package com.peakui.post.service;

import com.peakui.common.result.PageResponse;
import com.peakui.post.model.dto.CreateCommentRequest;
import com.peakui.post.model.dto.ApplyAiSummaryCommentRequest;
import com.peakui.post.model.dto.CreatePostRequest;
import com.peakui.post.model.dto.CreateReportRequest;
import com.peakui.post.model.dto.UpdatePostRequest;
import com.peakui.post.model.vo.CommentVO;
import com.peakui.post.model.vo.PostCategoryVO;
import com.peakui.post.model.vo.PostDetailVO;
import com.peakui.post.model.vo.PostListItemVO;
import com.peakui.post.model.vo.PostTagVO;

import java.util.List;

/**
 * 帖子业务接口。
 */
public interface PostService {

    PostDetailVO createPost(CreatePostRequest request);

    PostDetailVO updatePost(Long postId, UpdatePostRequest request);

    void deletePost(Long postId);

    PostDetailVO getPostDetail(Long postId);

    PageResponse<PostListItemVO> listPosts(Long categoryId, Integer status, String keyword, Long userId,
                                           Long pageNum, Long pageSize, String sortBy);

    void likePost(Long postId);

    void unlikePost(Long postId);

    void collectPost(Long postId, Long collectFolderId);

    void uncollectPost(Long postId);

    void sharePost(Long postId);

    PageResponse<PostListItemVO> listCollectedPosts(Long pageNum, Long pageSize);

    PageResponse<PostListItemVO> listLikedPosts(Long pageNum, Long pageSize);

    CommentVO createComment(Long postId, CreateCommentRequest request);

    List<CommentVO> listComments(Long postId);

    void deleteComment(Long postId, Long commentId);

    void likeComment(Long postId, Long commentId);

    void unlikeComment(Long postId, Long commentId);

    void reportPost(Long postId, CreateReportRequest request);

    void applyAiSummaryAndComment(Long postId, ApplyAiSummaryCommentRequest request);

    List<PostCategoryVO> listCategories();

    PageResponse<PostListItemVO> listDrafts(Long pageNum, Long pageSize);

    List<PostTagVO> listTags(String keyword);
}
