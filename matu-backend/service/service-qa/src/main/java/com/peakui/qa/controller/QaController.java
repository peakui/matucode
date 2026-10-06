package com.peakui.qa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.common.dashboard.DashboardQaStatsVO;
import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.qa.mapper.QaQuestionMapper;
import com.peakui.qa.model.dto.CreateAnswerRequest;
import com.peakui.qa.model.dto.CreateQaCommentRequest;
import com.peakui.qa.model.dto.CreateQuestionRequest;
import com.peakui.qa.model.dto.UpdateAnswerRequest;
import com.peakui.qa.model.dto.UpdateQuestionRequest;
import com.peakui.qa.model.entity.QaQuestion;
import com.peakui.qa.model.vo.QaAnswerVO;
import com.peakui.qa.model.vo.QaCategoryVO;
import com.peakui.qa.model.vo.QaCommentVO;
import com.peakui.qa.model.vo.QaQuestionDetailVO;
import com.peakui.qa.model.vo.QaQuestionListItemVO;
import com.peakui.qa.model.vo.QaWordCloudVO;
import com.peakui.qa.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 问答控制器。
 */
@Tag(name = "问答接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/qa")
public class QaController {

    private final QaService qaService;
    private final QaQuestionMapper qaQuestionMapper;

    @Operation(summary = "问答数据总览统计")
    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardQaStatsVO> dashboardStats() {
        Long questionCount = qaQuestionMapper.selectCount(null);
        Long resolvedCount = qaQuestionMapper.selectCount(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getStatus, 1)
                .or()
                .isNotNull(QaQuestion::getBestAnswerId));
        return ApiResponse.success(DashboardQaStatsVO.builder()
                .qaQuestionCount(questionCount)
                .qaResolvedCount(resolvedCount)
                .build());
    }

    @Operation(summary = "发布问题")
    @PostMapping("/questions")
    public ApiResponse<QaQuestionDetailVO> createQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ApiResponse.success(CommonError.PUBLISH_SUCCESS.message(), qaService.createQuestion(request));
    }

    @Operation(summary = "更新问题")
    @PutMapping("/questions/{questionId}")
    public ApiResponse<QaQuestionDetailVO> updateQuestion(@PathVariable Long questionId,
                                                          @Valid @RequestBody UpdateQuestionRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), qaService.updateQuestion(questionId, request));
    }

    @Operation(summary = "删除问题")
    @DeleteMapping("/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long questionId) {
        qaService.deleteQuestion(questionId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "问题详情")
    @GetMapping("/questions/{questionId}")
    public ApiResponse<QaQuestionDetailVO> getQuestionDetail(@PathVariable Long questionId) {
        return ApiResponse.success(qaService.getQuestionDetail(questionId));
    }

    @Operation(summary = "问题列表")
    @GetMapping("/questions")
    public ApiResponse<PageResponse<QaQuestionListItemVO>> listQuestions(@RequestParam(required = false) Long categoryId,
                                                                         @RequestParam(required = false) Integer status,
                                                                         @RequestParam(required = false) String keyword,
                                                                         @RequestParam(defaultValue = "1") Long pageNum,
                                                                         @RequestParam(defaultValue = "10") Long pageSize,
                                                                         @RequestParam(defaultValue = "latest") String sortBy) {
        return ApiResponse.success(qaService.listQuestions(categoryId, status, keyword, pageNum, pageSize, sortBy));
    }

    @Operation(summary = "我的提问")
    @GetMapping("/questions/mine")
    public ApiResponse<PageResponse<QaQuestionListItemVO>> listMyQuestions(@RequestParam(defaultValue = "1") Long pageNum,
                                                                           @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(qaService.listMyQuestions(pageNum, pageSize));
    }

    @Operation(summary = "我关注的问题")
    @GetMapping("/questions/following")
    public ApiResponse<PageResponse<QaQuestionListItemVO>> listMyFollowedQuestions(@RequestParam(defaultValue = "1") Long pageNum,
                                                                                    @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(qaService.listMyFollowedQuestions(pageNum, pageSize));
    }

    @Operation(summary = "我点赞的问题")
    @GetMapping("/questions/liked")
    public ApiResponse<PageResponse<QaQuestionListItemVO>> listLikedQuestions(@RequestParam(defaultValue = "1") Long pageNum,
                                                                              @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(qaService.listLikedQuestions(pageNum, pageSize));
    }

    @Operation(summary = "发布回答")
    @PostMapping("/questions/{questionId}/answers")
    public ApiResponse<QaAnswerVO> createAnswer(@PathVariable Long questionId,
                                                @Valid @RequestBody CreateAnswerRequest request) {
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), qaService.createAnswer(questionId, request));
    }

    @Operation(summary = "问题回答分页")
    @GetMapping("/questions/{questionId}/answers")
    public ApiResponse<PageResponse<QaAnswerVO>> listQuestionAnswers(@PathVariable Long questionId,
                                                                     @RequestParam(defaultValue = "1") Long pageNum,
                                                                     @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(qaService.listQuestionAnswers(questionId, pageNum, pageSize));
    }

    @Operation(summary = "更新回答")
    @PutMapping("/answers/{answerId}")
    public ApiResponse<QaAnswerVO> updateAnswer(@PathVariable Long answerId,
                                                @Valid @RequestBody UpdateAnswerRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), qaService.updateAnswer(answerId, request));
    }

    @Operation(summary = "我的回答")
    @GetMapping("/answers/mine")
    public ApiResponse<PageResponse<QaAnswerVO>> listMyAnswers(@RequestParam(defaultValue = "1") Long pageNum,
                                                               @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(qaService.listMyAnswers(pageNum, pageSize));
    }

    @Operation(summary = "删除回答")
    @DeleteMapping("/answers/{answerId}")
    public ApiResponse<Void> deleteAnswer(@PathVariable Long answerId) {
        qaService.deleteAnswer(answerId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "采纳回答")
    @PostMapping("/questions/{questionId}/answers/{answerId}/accept")
    public ApiResponse<Void> acceptAnswer(@PathVariable Long questionId, @PathVariable Long answerId) {
        qaService.acceptAnswer(questionId, answerId);
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), null);
    }

    @Operation(summary = "关注问题")
    @PostMapping("/questions/{questionId}/follow")
    public ApiResponse<Void> followQuestion(@PathVariable Long questionId) {
        qaService.followQuestion(questionId);
        return ApiResponse.success(CommonError.FOLLOW_SUCCESS.message(), null);
    }

    @Operation(summary = "取消关注问题")
    @PostMapping("/questions/{questionId}/unfollow")
    public ApiResponse<Void> unfollowQuestion(@PathVariable Long questionId) {
        qaService.unfollowQuestion(questionId);
        return ApiResponse.success(CommonError.UNFOLLOW_SUCCESS.message(), null);
    }

    @Operation(summary = "给问题投票")
    @PostMapping("/questions/{questionId}/vote")
    public ApiResponse<Void> voteQuestion(@PathVariable Long questionId, @RequestParam Integer voteType) {
        qaService.voteQuestion(questionId, voteType);
        return ApiResponse.success(CommonError.VOTE_SUCCESS.message(), null);
    }

    @Operation(summary = "取消问题投票")
    @DeleteMapping("/questions/{questionId}/vote")
    public ApiResponse<Void> unvoteQuestion(@PathVariable Long questionId) {
        qaService.unvoteQuestion(questionId);
        return ApiResponse.success(CommonError.VOTE_SUCCESS.message(), null);
    }

    @Operation(summary = "分享问题")
    @PostMapping("/questions/{questionId}/share")
    public ApiResponse<Void> shareQuestion(@PathVariable Long questionId) {
        qaService.shareQuestion(questionId);
        return ApiResponse.success("分享成功", null);
    }

    @Operation(summary = "给回答投票")
    @PostMapping("/answers/{answerId}/vote")
    public ApiResponse<Void> voteAnswer(@PathVariable Long answerId, @RequestParam Integer voteType) {
        qaService.voteAnswer(answerId, voteType);
        return ApiResponse.success(CommonError.VOTE_SUCCESS.message(), null);
    }

    @Operation(summary = "发表评论")
    @PostMapping("/comments")
    public ApiResponse<QaCommentVO> createComment(@Valid @RequestBody CreateQaCommentRequest request) {
        return ApiResponse.success(CommonError.COMMENT_SUCCESS.message(), qaService.createComment(request));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        qaService.deleteComment(commentId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<Void> likeComment(@PathVariable Long commentId) {
        qaService.likeComment(commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/comments/{commentId}/like")
    public ApiResponse<Void> unlikeComment(@PathVariable Long commentId) {
        qaService.unlikeComment(commentId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "评论列表")
    @GetMapping("/comments")
    public ApiResponse<List<QaCommentVO>> listComments(@RequestParam Integer targetType, @RequestParam Long targetId) {
        return ApiResponse.success(qaService.listComments(targetType, targetId));
    }

    @Operation(summary = "分类树")
    @GetMapping("/categories/tree")
    public ApiResponse<List<QaCategoryVO>> listCategoryTree() {
        return ApiResponse.success(qaService.listCategoryTree());
    }

    @Operation(summary = "分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<QaCategoryVO>> listCategories() {
        return ApiResponse.success(qaService.listCategories());
    }

    @Operation(summary = "获取问题词云")
    @GetMapping("/questions/{questionId}/word-cloud")
    public ApiResponse<QaWordCloudVO> getWordCloud(@PathVariable Long questionId) {
        return ApiResponse.success(qaService.getWordCloud(questionId));
    }
}
