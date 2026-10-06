package com.peakui.oj.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.oj.model.dto.AssociateOjProblemsRequest;
import com.peakui.oj.model.dto.CreateAssignmentRequest;
import com.peakui.oj.model.dto.CreateClassRequest;
import com.peakui.oj.model.dto.CreateDiscussionRequest;
import com.peakui.oj.model.dto.CreateOjProblemRequest;
import com.peakui.oj.model.dto.CreateOjTestCaseRequest;
import com.peakui.oj.model.dto.JoinClassRequest;
import com.peakui.oj.model.dto.SubmitOjCodeRequest;
import com.peakui.oj.model.dto.UpdateAssignmentRequest;
import com.peakui.oj.model.dto.UpdateClassRequest;
import com.peakui.oj.model.dto.UpdateOjProblemRequest;
import com.peakui.oj.model.vo.AssignmentDetailVO;
import com.peakui.oj.model.vo.AssignmentRankingVO;
import com.peakui.oj.model.vo.ClassAssignmentVO;
import com.peakui.oj.model.vo.ClassDiscussionVO;
import com.peakui.oj.model.vo.ClassMemberVO;
import com.peakui.oj.model.vo.ClassSubmissionVO;
import com.peakui.oj.model.vo.ClassVO;
import com.peakui.oj.model.vo.OjClassRankingVO;
import com.peakui.oj.model.vo.OjProblemVO;
import com.peakui.oj.model.vo.OjSolvedProblemCountVO;
import com.peakui.oj.model.vo.OjSubmissionDetailVO;
import com.peakui.oj.model.vo.OjSubmissionVO;
import com.peakui.oj.model.vo.OjTestCaseVO;
import com.peakui.oj.service.OjService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oj/classes")
@RequiredArgsConstructor
@Tag(name = "OJ模块接口")
public class OjController {

    private final OjService ojService;

    @Operation(summary = "创建班级")
    @PostMapping
    public ApiResponse<ClassVO> createClass(@Valid @RequestBody CreateClassRequest request) {
        return ApiResponse.success(ojService.createClass(request));
    }

    @Operation(summary = "更新班级")
    @PutMapping("/{classId}")
    public ApiResponse<ClassVO> updateClass(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                            @Valid @RequestBody UpdateClassRequest request) {
        return ApiResponse.success(ojService.updateClass(classId, request));
    }

    @Operation(summary = "获取班级详情")
    @GetMapping("/{classId}")
    public ApiResponse<ClassVO> getClassDetail(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId) {
        return ApiResponse.success(ojService.getClassDetail(classId));
    }

    @Operation(summary = "分页查询班级列表")
    @GetMapping
    public ApiResponse<PageResponse<ClassVO>> listClasses(@Parameter(description = "关键词", example = "Java") @RequestParam(required = false) String keyword,
                                                          @Parameter(description = "状态", example = "1") @RequestParam(required = false) Integer status,
                                                          @Parameter(description = "页码", example = "1") @RequestParam(required = false) Long pageNum,
                                                          @Parameter(description = "每页大小", example = "10") @RequestParam(required = false) Long pageSize) {
        return ApiResponse.success(ojService.listClasses(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "加入班级")
    @PostMapping("/{classId}/join")
    public ApiResponse<Void> joinClass(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                       @Valid @RequestBody JoinClassRequest request) {
        ojService.joinClass(classId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "审核通过班级成员")
    @PostMapping("/{classId}/members/{memberId}/approve")
    public ApiResponse<Void> approveMember(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                           @Parameter(description = "成员ID", example = "2") @PathVariable Long memberId) {
        ojService.approveMember(classId, memberId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "审核拒绝班级成员")
    @PostMapping("/{classId}/members/{memberId}/reject")
    public ApiResponse<Void> rejectMember(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                          @Parameter(description = "成员ID", example = "2") @PathVariable Long memberId) {
        ojService.rejectMember(classId, memberId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "移除班级成员")
    @DeleteMapping("/{classId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                          @Parameter(description = "成员ID", example = "2") @PathVariable Long memberId) {
        ojService.removeMember(classId, memberId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询班级成员列表")
    @GetMapping("/{classId}/members")
    public ApiResponse<List<ClassMemberVO>> listMembers(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId) {
        return ApiResponse.success(ojService.listMembers(classId));
    }

    @Operation(summary = "获取班级/竞赛 ACM 排行榜")
    @GetMapping("/{classId}/ranking")
    public ApiResponse<OjClassRankingVO> getClassRanking(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId) {
        return ApiResponse.success(ojService.getClassRanking(classId));
    }

    @Operation(summary = "创建班级作业")
    @PostMapping("/{classId}/assignments")
    public ApiResponse<ClassAssignmentVO> createAssignment(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                           @Valid @RequestBody CreateAssignmentRequest request) {
        return ApiResponse.success(ojService.createAssignment(classId, request));
    }

    @Operation(summary = "更新班级作业")
    @PutMapping("/{classId}/assignments/{assignmentId}")
    public ApiResponse<ClassAssignmentVO> updateAssignment(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                           @Parameter(description = "作业ID", example = "1") @PathVariable Long assignmentId,
                                                           @Valid @RequestBody UpdateAssignmentRequest request) {
        return ApiResponse.success(ojService.updateAssignment(classId, assignmentId, request));
    }

    @Operation(summary = "查询班级作业列表")
    @GetMapping("/{classId}/assignments")
    public ApiResponse<List<ClassAssignmentVO>> listAssignments(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId) {
        return ApiResponse.success(ojService.listAssignments(classId));
    }

    @Operation(summary = "获取作业详情")
    @GetMapping("/{classId}/assignments/{assignmentId}")
    public ApiResponse<AssignmentDetailVO> getAssignmentDetail(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                              @Parameter(description = "作业ID", example = "1") @PathVariable Long assignmentId) {
        return ApiResponse.success(ojService.getAssignmentDetail(classId, assignmentId));
    }

    @Operation(summary = "获取作业排行榜")
    @GetMapping("/{classId}/assignments/{assignmentId}/ranking")
    public ApiResponse<AssignmentRankingVO> getAssignmentRanking(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                                 @Parameter(description = "作业ID", example = "1") @PathVariable Long assignmentId) {
        return ApiResponse.success(ojService.getAssignmentRanking(classId, assignmentId));
    }

    @Operation(summary = "分页查询作业提交记录")
    @GetMapping("/{classId}/assignments/{assignmentId}/submissions")
    public ApiResponse<PageResponse<ClassSubmissionVO>> listAssignmentSubmissions(
            @Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
            @Parameter(description = "作业ID", example = "1") @PathVariable Long assignmentId,
            @Parameter(description = "用户ID", example = "1") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态", example = "1") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码", example = "1") @RequestParam(required = false) Long pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(required = false) Long pageSize) {
        return ApiResponse.success(ojService.listAssignmentSubmissions(
                classId, assignmentId, userId, status, pageNum, pageSize));
    }

    @Operation(summary = "创建班级讨论")
    @PostMapping("/{classId}/discussions")
    public ApiResponse<ClassDiscussionVO> createDiscussion(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                           @Valid @RequestBody CreateDiscussionRequest request) {
        return ApiResponse.success(ojService.createDiscussion(classId, request));
    }

    @Operation(summary = "查询班级讨论列表")
    @GetMapping("/{classId}/discussions")
    public ApiResponse<List<ClassDiscussionVO>> listDiscussions(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                                @Parameter(description = "作业ID", example = "1") @RequestParam(required = false) Long assignmentId) {
        return ApiResponse.success(ojService.listDiscussions(classId, assignmentId));
    }

    @Operation(summary = "提交OJ代码")
    @PostMapping("/submissions")
    public ApiResponse<OjSubmissionVO> submitOjCode(@Valid @RequestBody SubmitOjCodeRequest request) {
        return ApiResponse.success(ojService.submitOjCode(request));
    }

    @Operation(summary = "创建OJ题目")
    @PostMapping("/problems")
    public ApiResponse<OjProblemVO> createProblem(@Valid @RequestBody CreateOjProblemRequest request) {
        return ApiResponse.success(ojService.createProblem(request));
    }

    @Operation(summary = "更新OJ题目")
    @PutMapping("/problems/{problemId}")
    public ApiResponse<OjProblemVO> updateProblem(@Parameter(description = "题目ID", example = "1") @PathVariable Long problemId,
                                                  @Valid @RequestBody UpdateOjProblemRequest request) {
        return ApiResponse.success(ojService.updateProblem(problemId, request));
    }

    @Operation(summary = "获取OJ题目详情")
    @GetMapping("/problems/{problemId}")
    public ApiResponse<OjProblemVO> getProblemDetail(@Parameter(description = "题目ID", example = "1") @PathVariable Long problemId) {
        return ApiResponse.success(ojService.getProblemDetail(problemId));
    }

    @Operation(summary = "分页查询OJ题目列表")
    @GetMapping("/problems")
    public ApiResponse<PageResponse<OjProblemVO>> listProblems(@Parameter(description = "关键词", example = "A+B") @RequestParam(required = false) String keyword,
                                                               @Parameter(description = "难度", example = "1") @RequestParam(required = false) Integer difficulty,
                                                               @Parameter(description = "状态", example = "1") @RequestParam(required = false) Integer status,
                                                               @Parameter(description = "页码", example = "1") @RequestParam(required = false) Long pageNum,
                                                               @Parameter(description = "每页大小", example = "10") @RequestParam(required = false) Long pageSize) {
        return ApiResponse.success(ojService.listProblems(keyword, difficulty, status, pageNum, pageSize));
    }

    @Operation(summary = "分页查询班级题目列表")
    @GetMapping("/{classId}/problems")
    public ApiResponse<PageResponse<OjProblemVO>> listClassProblems(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                                    @Parameter(description = "关键词", example = "A+B") @RequestParam(required = false) String keyword,
                                                                    @Parameter(description = "难度", example = "1") @RequestParam(required = false) Integer difficulty,
                                                                    @Parameter(description = "状态", example = "1") @RequestParam(required = false) Integer status,
                                                                    @Parameter(description = "页码", example = "1") @RequestParam(required = false) Long pageNum,
                                                                    @Parameter(description = "每页大小", example = "10") @RequestParam(required = false) Long pageSize) {
        return ApiResponse.success(ojService.listClassProblems(classId, keyword, difficulty, status, pageNum, pageSize));
    }

    @Operation(summary = "获取班级题目详情")
    @GetMapping("/{classId}/problems/{problemId}")
    public ApiResponse<OjProblemVO> getClassProblemDetail(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                          @Parameter(description = "题目ID", example = "1") @PathVariable Long problemId) {
        return ApiResponse.success(ojService.getClassProblemDetail(classId, problemId));
    }

    @Operation(summary = "创建班级题目")
    @PostMapping("/{classId}/problems")
    public ApiResponse<OjProblemVO> createClassProblem(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                       @Valid @RequestBody CreateOjProblemRequest request) {
        return ApiResponse.success(ojService.createClassProblem(classId, request));
    }

    @Operation(summary = "更新班级题目")
    @PutMapping("/{classId}/problems/{problemId}")
    public ApiResponse<OjProblemVO> updateClassProblem(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                       @Parameter(description = "题目ID", example = "1") @PathVariable Long problemId,
                                                       @Valid @RequestBody UpdateOjProblemRequest request) {
        return ApiResponse.success(ojService.updateClassProblem(classId, problemId, request));
    }

    @Operation(summary = "关联已有题目到班级")
    @PostMapping("/{classId}/problems/associate")
    public ApiResponse<Void> associateClassProblems(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                    @Valid @RequestBody AssociateOjProblemsRequest request) {
        ojService.associateClassProblems(classId, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "移除班级题目")
    @DeleteMapping("/{classId}/problems/{problemId}")
    public ApiResponse<Void> removeClassProblem(@Parameter(description = "班级ID", example = "1") @PathVariable Long classId,
                                                @Parameter(description = "题目ID", example = "1") @PathVariable Long problemId) {
        ojService.removeClassProblem(classId, problemId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "新增OJ测试用例")
    @PostMapping("/problems/{problemId}/test-cases")
    public ApiResponse<OjTestCaseVO> addTestCase(@Parameter(description = "题目ID", example = "1") @PathVariable Long problemId,
                                                 @Valid @RequestBody CreateOjTestCaseRequest request) {
        return ApiResponse.success(ojService.addTestCase(problemId, request));
    }

    @Operation(summary = "查询OJ测试用例列表")
    @GetMapping("/problems/{problemId}/test-cases")
    public ApiResponse<List<OjTestCaseVO>> listTestCases(@Parameter(description = "题目ID", example = "1") @PathVariable Long problemId) {
        return ApiResponse.success(ojService.listTestCases(problemId));
    }

    @Operation(summary = "分页查询提交记录")
    @GetMapping("/submissions")
    public ApiResponse<PageResponse<OjSubmissionVO>> listSubmissions(@Parameter(description = "题目ID", example = "1") @RequestParam(required = false) Long problemId,
                                                                     @Parameter(description = "用户ID", example = "1001") @RequestParam(required = false) Long userId,
                                                                     @Parameter(description = "判题状态", example = "1") @RequestParam(required = false) Integer status,
                                                                     @Parameter(description = "页码", example = "1") @RequestParam(required = false) Long pageNum,
                                                                     @Parameter(description = "每页大小", example = "20") @RequestParam(required = false) Long pageSize) {
        return ApiResponse.success(ojService.listSubmissions(problemId, userId, status, pageNum, pageSize));
    }

    @Operation(summary = "获取刷题数量")
    @GetMapping("/statistics/solved-count")
    public ApiResponse<OjSolvedProblemCountVO> getSolvedProblemCount(@Parameter(description = "用户ID", example = "1001") @RequestParam(required = false) Long userId) {
        return ApiResponse.success(ojService.getSolvedProblemCount(userId));
    }

    @Operation(summary = "获取提交详情")
    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<OjSubmissionVO> getSubmissionDetail(@Parameter(description = "提交ID", example = "101") @PathVariable Long submissionId) {
        return ApiResponse.success(ojService.getSubmissionDetail(submissionId));
    }

    @Operation(summary = "查询提交测试点详情")
    @GetMapping("/submissions/{submissionId}/details")
    public ApiResponse<List<OjSubmissionDetailVO>> listSubmissionDetails(@Parameter(description = "提交ID", example = "101") @PathVariable Long submissionId) {
        return ApiResponse.success(ojService.listSubmissionDetails(submissionId));
    }
}
