package com.peakui.qa.service;

import com.peakui.common.result.PageResponse;
import com.peakui.qa.model.dto.CreateAnswerRequest;
import com.peakui.qa.model.dto.CreateQaCommentRequest;
import com.peakui.qa.model.dto.CreateQuestionRequest;
import com.peakui.qa.model.dto.UpdateAnswerRequest;
import com.peakui.qa.model.dto.UpdateQuestionRequest;
import com.peakui.qa.model.vo.QaAnswerVO;
import com.peakui.qa.model.vo.QaCategoryVO;
import com.peakui.qa.model.vo.QaCommentVO;
import com.peakui.qa.model.vo.QaQuestionDetailVO;
import com.peakui.qa.model.vo.QaQuestionListItemVO;
import com.peakui.qa.model.vo.QaWordCloudVO;

import java.util.List;

/**
 * 问答业务接口。
 */
public interface QaService {

    QaQuestionDetailVO createQuestion(CreateQuestionRequest request);

    QaQuestionDetailVO updateQuestion(Long questionId, UpdateQuestionRequest request);

    void deleteQuestion(Long questionId);

    QaQuestionDetailVO getQuestionDetail(Long questionId);

    PageResponse<QaQuestionListItemVO> listQuestions(Long categoryId, Integer status, String keyword,
                                                     Long pageNum, Long pageSize, String sortBy);

    PageResponse<QaQuestionListItemVO> listMyQuestions(Long pageNum, Long pageSize);

    PageResponse<QaQuestionListItemVO> listLikedQuestions(Long pageNum, Long pageSize);

    PageResponse<QaQuestionListItemVO> listMyFollowedQuestions(Long pageNum, Long pageSize);

    PageResponse<QaAnswerVO> listQuestionAnswers(Long questionId, Long pageNum, Long pageSize);

    QaAnswerVO createAnswer(Long questionId, CreateAnswerRequest request);

    QaAnswerVO updateAnswer(Long answerId, UpdateAnswerRequest request);

    PageResponse<QaAnswerVO> listMyAnswers(Long pageNum, Long pageSize);

    void deleteAnswer(Long answerId);

    void acceptAnswer(Long questionId, Long answerId);

    void followQuestion(Long questionId);

    void unfollowQuestion(Long questionId);

    void shareQuestion(Long questionId);

    void voteQuestion(Long questionId, Integer voteType);

    void unvoteQuestion(Long questionId);

    void voteAnswer(Long answerId, Integer voteType);

    QaCommentVO createComment(CreateQaCommentRequest request);

    void deleteComment(Long commentId);

    void likeComment(Long commentId);

    void unlikeComment(Long commentId);

    List<QaCommentVO> listComments(Integer targetType, Long targetId);

    List<QaCategoryVO> listCategoryTree();

    List<QaCategoryVO> listCategories();

    QaWordCloudVO getWordCloud(Long questionId);
}
