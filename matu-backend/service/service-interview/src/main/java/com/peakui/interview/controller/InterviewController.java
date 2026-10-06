package com.peakui.interview.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.common.dashboard.DashboardInterviewStatsDTO;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.interview.mapper.InterviewCategoryMapper;
import com.peakui.interview.mapper.InterviewCompanyMapper;
import com.peakui.interview.mapper.InterviewQuestionMapper;
import com.peakui.interview.model.dto.CreateInterviewAnswerRequest;
import com.peakui.interview.model.dto.CreateInterviewCategoryRequest;
import com.peakui.interview.model.dto.CreateInterviewQuestionRequest;
import com.peakui.interview.model.dto.CreateMockInterviewRequest;
import com.peakui.interview.model.dto.UpdateInterviewCategoryRequest;
import com.peakui.interview.model.dto.UpdateInterviewQuestionRequest;
import com.peakui.interview.model.dto.UpdateQuestionProgressRequest;
import com.peakui.interview.model.entity.InterviewCategory;
import com.peakui.interview.model.entity.InterviewCompany;
import com.peakui.interview.model.entity.InterviewQuestion;
import com.peakui.interview.model.vo.InterviewAnswerVO;
import com.peakui.interview.model.vo.InterviewCategoryVO;
import com.peakui.interview.model.vo.InterviewCompanyVO;
import com.peakui.interview.model.vo.InterviewQuestionVO;
import com.peakui.interview.model.vo.MockInterviewVO;
import com.peakui.interview.model.vo.UserQuestionProgressVO;
import com.peakui.interview.model.vo.UserWrongQuestionVO;
import com.peakui.interview.service.InterviewService;
import com.peakui.common.exception.CommonError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "面试接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final InterviewCategoryMapper interviewCategoryMapper;
    private final InterviewCompanyMapper interviewCompanyMapper;

    @Operation(summary = "面试数据总览统计")
    @GetMapping("/internal/dashboard/stats")
    public ApiResponse<DashboardInterviewStatsDTO> dashboardStats() {
        return ApiResponse.success(DashboardInterviewStatsDTO.builder()
                .questionCount(interviewQuestionMapper.selectCount(new LambdaQueryWrapper<InterviewQuestion>().eq(InterviewQuestion::getStatus, 1)))
                .categoryCount(interviewCategoryMapper.selectCount(new LambdaQueryWrapper<InterviewCategory>().eq(InterviewCategory::getStatus, 1)))
                .companyCount(interviewCompanyMapper.selectCount(new LambdaQueryWrapper<InterviewCompany>().eq(InterviewCompany::getStatus, 1)))
                .lockedQuestionCount(interviewQuestionMapper.selectCount(new LambdaQueryWrapper<InterviewQuestion>().eq(InterviewQuestion::getIsLocked, 1)))
                .build());
    }

    @Operation(summary = "创建面试题")
    @PostMapping("/questions")
    public ApiResponse<InterviewQuestionVO> createQuestion(@Valid @RequestBody CreateInterviewQuestionRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), interviewService.createQuestion(request));
    }

    @Operation(summary = "更新面试题")
    @PutMapping("/questions/{questionId}")
    public ApiResponse<InterviewQuestionVO> updateQuestion(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId,
                                                           @Valid @RequestBody UpdateInterviewQuestionRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), interviewService.updateQuestion(questionId, request));
    }

    @Operation(summary = "面试题详情")
    @GetMapping("/questions/{questionId}")
    public ApiResponse<InterviewQuestionVO> getQuestionDetail(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        return ApiResponse.success(interviewService.getQuestionDetail(questionId));
    }

    @Operation(summary = "面试题分页列表")
    @GetMapping("/questions")
    public ApiResponse<PageResponse<InterviewQuestionVO>> listQuestions(@Parameter(description = "分类ID", example = "1") @RequestParam(required = false) Long categoryId,
                                                                        @Parameter(description = "公司ID", example = "1") @RequestParam(required = false) Long companyId,
                                                                        @Parameter(description = "难度 1简单 2中等 3困难", example = "2") @RequestParam(required = false) Integer difficulty,
                                                                        @Parameter(description = "是否锁定 0否 1是", example = "0") @RequestParam(required = false) Integer isLocked,
                                                                        @Parameter(description = "关键词", example = "Java") @RequestParam(required = false) String keyword,
                                                                        @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
                                                                        @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(interviewService.listQuestions(categoryId, companyId, difficulty, isLocked, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增题目答案")
    @PostMapping("/questions/{questionId}/answers")
    public ApiResponse<InterviewAnswerVO> createAnswer(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId,
                                                       @Valid @RequestBody CreateInterviewAnswerRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), interviewService.createAnswer(questionId, request));
    }

    @Operation(summary = "题目答案列表")
    @GetMapping("/questions/{questionId}/answers")
    public ApiResponse<List<InterviewAnswerVO>> listAnswers(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        return ApiResponse.success(interviewService.listAnswers(questionId));
    }

    @Operation(summary = "创建面试分类")
    @PostMapping("/categories")
    public ApiResponse<InterviewCategoryVO> createCategory(@Valid @RequestBody CreateInterviewCategoryRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), interviewService.createCategory(request));
    }

    @Operation(summary = "更新面试分类")
    @PutMapping("/categories/{categoryId}")
    public ApiResponse<InterviewCategoryVO> updateCategory(@Parameter(description = "分类ID", example = "1") @PathVariable Long categoryId,
                                                           @Valid @RequestBody UpdateInterviewCategoryRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), interviewService.updateCategory(categoryId, request));
    }

    @Operation(summary = "删除面试分类")
    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@Parameter(description = "分类ID", example = "1") @PathVariable Long categoryId) {
        interviewService.deleteCategory(categoryId);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }

    @Operation(summary = "分类管理分页查询")
    @GetMapping("/categories/page")
    public ApiResponse<PageResponse<InterviewCategoryVO>> pageCategories(@Parameter(description = "父分类ID", example = "0") @RequestParam(required = false) Long parentId,
                                                                         @Parameter(description = "状态 0禁用 1启用", example = "1") @RequestParam(required = false) Integer status,
                                                                         @Parameter(description = "关键词", example = "Java") @RequestParam(required = false) String keyword,
                                                                         @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
                                                                         @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(interviewService.pageCategories(parentId, status, keyword, pageNum, pageSize));
    }

    @Operation(summary = "面试分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<InterviewCategoryVO>> listCategories() {
        return ApiResponse.success(interviewService.listCategories());
    }

    @Operation(summary = "公司列表")
    @GetMapping("/companies")
    public ApiResponse<List<InterviewCompanyVO>> listCompanies() {
        return ApiResponse.success(interviewService.listCompanies());
    }

    @Operation(summary = "更新用户题目进度")
    @PutMapping("/questions/{questionId}/progress")
    public ApiResponse<UserQuestionProgressVO> updateProgress(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId,
                                                              @Valid @RequestBody UpdateQuestionProgressRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), interviewService.updateProgress(questionId, request));
    }

    @Operation(summary = "获取用户题目进度")
    @GetMapping("/questions/{questionId}/progress")
    public ApiResponse<UserQuestionProgressVO> getProgress(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        return ApiResponse.success(interviewService.getProgress(questionId));
    }

    @Operation(summary = "收藏题目")
    @PostMapping("/questions/{questionId}/collect")
    public ApiResponse<Void> collectQuestion(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        interviewService.collectQuestion(questionId);
        return ApiResponse.success(CommonError.COLLECT_SUCCESS.message(), null);
    }

    @Operation(summary = "取消收藏题目")
    @PostMapping("/questions/{questionId}/uncollect")
    public ApiResponse<Void> uncollectQuestion(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        interviewService.uncollectQuestion(questionId);
        return ApiResponse.success(CommonError.UNCOLLECT_SUCCESS.message(), null);
    }

    @Operation(summary = "我的收藏题目")
    @GetMapping("/questions/collected")
    public ApiResponse<PageResponse<InterviewQuestionVO>> listCollectedQuestions(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
                                                                                 @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(interviewService.listCollectedQuestions(pageNum, pageSize));
    }

    @Operation(summary = "错题本列表")
    @GetMapping("/wrong-questions")
    public ApiResponse<PageResponse<UserWrongQuestionVO>> listWrongQuestions(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
                                                                             @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize,
                                                                             @Parameter(description = "是否已解决 0未解决 1已解决", example = "0") @RequestParam(required = false) Integer isResolved) {
        return ApiResponse.success(interviewService.listWrongQuestions(pageNum, pageSize, isResolved));
    }

    @Operation(summary = "标记错题已解决")
    @PostMapping("/questions/{questionId}/wrong-questions/resolve")
    public ApiResponse<Void> resolveWrongQuestion(@Parameter(description = "题目ID", example = "1") @PathVariable Long questionId) {
        interviewService.resolveWrongQuestion(questionId);
        return ApiResponse.success(CommonError.SUBMIT_SUCCESS.message(), null);
    }

    @Operation(summary = "创建模拟面试")
    @PostMapping("/mock-interviews")
    public ApiResponse<MockInterviewVO> createMockInterview(@Valid @RequestBody CreateMockInterviewRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), interviewService.createMockInterview(request));
    }

    @Operation(summary = "模拟面试分页列表")
    @GetMapping("/mock-interviews")
    public ApiResponse<PageResponse<MockInterviewVO>> listMockInterviews(@Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long pageNum,
                                                                         @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(interviewService.listMockInterviews(pageNum, pageSize));
    }
}
