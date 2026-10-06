package com.peakui.oj.service;

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
import com.peakui.oj.model.vo.ClassSubmissionVO;
import com.peakui.oj.model.vo.ClassMemberVO;
import com.peakui.oj.model.vo.ClassVO;
import com.peakui.oj.model.vo.OjClassRankingVO;
import com.peakui.oj.model.vo.OjProblemVO;
import com.peakui.oj.model.vo.OjSolvedProblemCountVO;
import com.peakui.oj.model.vo.OjSubmissionDetailVO;
import com.peakui.oj.model.vo.OjSubmissionVO;
import com.peakui.oj.model.vo.OjTestCaseVO;

import java.util.List;

public interface OjService {

    ClassVO createClass(CreateClassRequest request);

    ClassVO updateClass(Long classId, UpdateClassRequest request);

    ClassVO getClassDetail(Long classId);

    PageResponse<ClassVO> listClasses(
            String keyword,
            Integer status,
            Long pageNum,
            Long pageSize
    );

    void joinClass(Long classId, JoinClassRequest request);

    void approveMember(Long classId, Long memberId);

    void rejectMember(Long classId, Long memberId);

    void removeMember(Long classId, Long memberId);

    List<ClassMemberVO> listMembers(Long classId);

    ClassAssignmentVO createAssignment(Long classId, CreateAssignmentRequest request);

    ClassAssignmentVO updateAssignment(
            Long classId,
            Long assignmentId,
            UpdateAssignmentRequest request
    );

    List<ClassAssignmentVO> listAssignments(Long classId);

    AssignmentDetailVO getAssignmentDetail(Long classId, Long assignmentId);

    AssignmentRankingVO getAssignmentRanking(Long classId, Long assignmentId);

    PageResponse<ClassSubmissionVO> listAssignmentSubmissions(
            Long classId,
            Long assignmentId,
            Long userId,
            Integer status,
            Long pageNum,
            Long pageSize
    );

    ClassDiscussionVO createDiscussion(Long classId, CreateDiscussionRequest request);

    List<ClassDiscussionVO> listDiscussions(Long classId, Long assignmentId);

    OjSubmissionVO submitOjCode(SubmitOjCodeRequest request);

    OjProblemVO createProblem(CreateOjProblemRequest request);

    OjProblemVO updateProblem(Long problemId, UpdateOjProblemRequest request);

    OjProblemVO getProblemDetail(Long problemId);

    PageResponse<OjProblemVO> listProblems(
            String keyword,
            Integer difficulty,
            Integer status,
            Long pageNum,
            Long pageSize
    );

    PageResponse<OjProblemVO> listClassProblems(
            Long classId,
            String keyword,
            Integer difficulty,
            Integer status,
            Long pageNum,
            Long pageSize
    );

    OjProblemVO getClassProblemDetail(Long classId, Long problemId);

    OjClassRankingVO getClassRanking(Long classId);

    OjProblemVO createClassProblem(Long classId, CreateOjProblemRequest request);

    OjProblemVO updateClassProblem(Long classId, Long problemId, UpdateOjProblemRequest request);

    void associateClassProblems(Long classId, AssociateOjProblemsRequest request);

    void removeClassProblem(Long classId, Long problemId);

    OjTestCaseVO addTestCase(Long problemId, CreateOjTestCaseRequest request);

    List<OjTestCaseVO> listTestCases(Long problemId);

    PageResponse<OjSubmissionVO> listSubmissions(
            Long problemId,
            Long userId,
            Integer status,
            Long pageNum,
            Long pageSize
    );

    OjSolvedProblemCountVO getSolvedProblemCount(Long userId);

    OjSubmissionVO getSubmissionDetail(Long submissionId);

    List<OjSubmissionDetailVO> listSubmissionDetails(Long submissionId);
}
