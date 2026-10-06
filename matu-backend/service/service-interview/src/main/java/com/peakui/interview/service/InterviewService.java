package com.peakui.interview.service;

import com.peakui.common.result.PageResponse;
import com.peakui.interview.model.dto.CreateInterviewAnswerRequest;
import com.peakui.interview.model.dto.CreateInterviewCategoryRequest;
import com.peakui.interview.model.dto.CreateInterviewQuestionRequest;
import com.peakui.interview.model.dto.CreateMockInterviewRequest;
import com.peakui.interview.model.dto.UpdateInterviewCategoryRequest;
import com.peakui.interview.model.dto.UpdateInterviewQuestionRequest;
import com.peakui.interview.model.dto.UpdateQuestionProgressRequest;
import com.peakui.interview.model.vo.InterviewAnswerVO;
import com.peakui.interview.model.vo.InterviewCategoryVO;
import com.peakui.interview.model.vo.InterviewCompanyVO;
import com.peakui.interview.model.vo.InterviewQuestionVO;
import com.peakui.interview.model.vo.MockInterviewVO;
import com.peakui.interview.model.vo.UserQuestionProgressVO;
import com.peakui.interview.model.vo.UserWrongQuestionVO;

import java.util.List;

public interface InterviewService {

    InterviewQuestionVO createQuestion(CreateInterviewQuestionRequest request);

    InterviewQuestionVO updateQuestion(Long questionId, UpdateInterviewQuestionRequest request);

    InterviewQuestionVO getQuestionDetail(Long questionId);

    PageResponse<InterviewQuestionVO> listQuestions(Long categoryId, Long companyId, Integer difficulty,
                                                    Integer isLocked, String keyword, Long pageNum, Long pageSize);

    InterviewAnswerVO createAnswer(Long questionId, CreateInterviewAnswerRequest request);

    List<InterviewAnswerVO> listAnswers(Long questionId);

    InterviewCategoryVO createCategory(CreateInterviewCategoryRequest request);

    InterviewCategoryVO updateCategory(Long categoryId, UpdateInterviewCategoryRequest request);

    void deleteCategory(Long categoryId);

    PageResponse<InterviewCategoryVO> pageCategories(Long parentId, Integer status, String keyword, Long pageNum, Long pageSize);

    List<InterviewCategoryVO> listCategories();

    List<InterviewCompanyVO> listCompanies();

    UserQuestionProgressVO updateProgress(Long questionId, UpdateQuestionProgressRequest request);

    UserQuestionProgressVO getProgress(Long questionId);

    void collectQuestion(Long questionId);

    void uncollectQuestion(Long questionId);

    PageResponse<InterviewQuestionVO> listCollectedQuestions(Long pageNum, Long pageSize);

    PageResponse<UserWrongQuestionVO> listWrongQuestions(Long pageNum, Long pageSize, Integer isResolved);

    void resolveWrongQuestion(Long questionId);

    MockInterviewVO createMockInterview(CreateMockInterviewRequest request);

    PageResponse<MockInterviewVO> listMockInterviews(Long pageNum, Long pageSize);
}
