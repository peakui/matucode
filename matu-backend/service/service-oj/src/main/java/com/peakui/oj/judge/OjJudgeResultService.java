package com.peakui.oj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.oj.mapper.ClassSubmissionMapper;
import com.peakui.oj.mapper.OjProblemMapper;
import com.peakui.oj.mapper.OjSubmissionDetailMapper;
import com.peakui.oj.mapper.OjSubmissionMapper;
import com.peakui.oj.model.entity.ClassSubmission;
import com.peakui.oj.model.entity.OjProblem;
import com.peakui.oj.model.entity.OjSubmission;
import com.peakui.oj.model.entity.OjSubmissionDetail;
import com.peakui.oj.model.vo.OjSubmissionVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class OjJudgeResultService {
    /** class_submissions.status：已完成/AC。 */
    private static final int CLASS_SUBMISSION_AC = 1;
    /** 每通过一道作业题目计 100 分。 */
    private static final BigDecimal SCORE_PER_PROBLEM = new BigDecimal("100");

    private final OjSubmissionMapper ojSubmissionMapper;
    private final OjProblemMapper ojProblemMapper;
    private final OjSubmissionDetailMapper ojSubmissionDetailMapper;
    private final ClassSubmissionMapper classSubmissionMapper;

    public OjJudgeResultService(OjSubmissionMapper ojSubmissionMapper, OjProblemMapper ojProblemMapper,
                                OjSubmissionDetailMapper ojSubmissionDetailMapper,
                                ClassSubmissionMapper classSubmissionMapper) {
        this.ojSubmissionMapper = ojSubmissionMapper;
        this.ojProblemMapper = ojProblemMapper;
        this.ojSubmissionDetailMapper = ojSubmissionDetailMapper;
        this.classSubmissionMapper = classSubmissionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public OjSubmissionVO saveJudgeResult(OjSubmission submission, OjProblem problem, JudgeResult result) {
        OjSubmission currentSubmission = ojSubmissionMapper.selectById(submission.getId());
        if (currentSubmission == null) {
            throw new IllegalStateException("提交记录不存在");
        }
        if (!Objects.equals(currentSubmission.getStatus(), JudgeStatusEnum.WAITING.getCode())) {
            return submissionVO(currentSubmission);
        }

        ojSubmissionDetailMapper.delete(new LambdaQueryWrapper<OjSubmissionDetail>().eq(OjSubmissionDetail::getSubmissionId, submission.getId()));
        if (result.getCaseResults() != null) {
            for (JudgeCaseResult caseResult : result.getCaseResults()) {
                OjSubmissionDetail detail = new OjSubmissionDetail();
                detail.setSubmissionId(submission.getId());
                detail.setCaseNo(caseResult.getCaseNo());
                detail.setInput(caseResult.getInput());
                detail.setExpectedOutput(caseResult.getExpectedOutput());
                detail.setActualOutput(caseResult.getActualOutput());
                detail.setStatus(caseResult.getStatus());
                detail.setExecutionTime(caseResult.getExecutionTime());
                detail.setMemoryUsed(caseResult.getMemoryUsed());
                detail.setErrorMessage(caseResult.getErrorMessage());
                detail.setCreatedAt(LocalDateTime.now());
                ojSubmissionDetailMapper.insert(detail);
            }
        }

        currentSubmission.setStatus(result.getStatus());
        currentSubmission.setExecutionTime(result.getExecutionTime());
        currentSubmission.setMemoryUsed(result.getMemoryUsed());
        currentSubmission.setPassedCases(result.getPassedCases());
        currentSubmission.setTotalCases(result.getTotalCases());
        currentSubmission.setPassRate(result.getPassRate() == null ? BigDecimal.ZERO : result.getPassRate());
        currentSubmission.setErrorMessage(result.getErrorMessage());
        currentSubmission.setJudgeTime(LocalDateTime.now());
        ojSubmissionMapper.updateById(currentSubmission);
        updateProblemStats(problem, result);
        recordClassSubmission(currentSubmission, result);
        return submissionVO(currentSubmission);
    }

    /**
     * 作业提交判题后记账：仅在 AC 时写入/更新 class_submissions（已完成的题目）。
     * 非 AC 尝试不落此表，提交次数由 oj_submissions 体现。
     */
    private void recordClassSubmission(OjSubmission submission, JudgeResult result) {
        if (submission.getAssignmentId() == null
                || result.getStatus() == null
                || !result.getStatus().equals(JudgeStatusEnum.ACCEPTED.getCode())) {
            return;
        }
        ClassSubmission existing = classSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassSubmission>()
                .eq(ClassSubmission::getAssignmentId, submission.getAssignmentId())
                .eq(ClassSubmission::getUserId, submission.getUserId())
                .eq(ClassSubmission::getProblemId, submission.getProblemId())
                .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(CLASS_SUBMISSION_AC);
            existing.setScore(SCORE_PER_PROBLEM);
            existing.setSubmissionId(submission.getId());
            if (existing.getSubmittedAt() == null) {
                existing.setSubmittedAt(now);
            }
            classSubmissionMapper.updateById(existing);
            return;
        }
        ClassSubmission record = new ClassSubmission();
        record.setAssignmentId(submission.getAssignmentId());
        record.setUserId(submission.getUserId());
        record.setProblemId(submission.getProblemId());
        record.setSubmissionId(submission.getId());
        record.setStatus(CLASS_SUBMISSION_AC);
        record.setScore(SCORE_PER_PROBLEM);
        record.setSubmittedAt(now);
        record.setCreatedAt(now);
        try {
            classSubmissionMapper.insert(record);
        } catch (DuplicateKeyException e) {
            ClassSubmission concurrent = classSubmissionMapper.selectOne(new LambdaQueryWrapper<ClassSubmission>()
                    .eq(ClassSubmission::getAssignmentId, submission.getAssignmentId())
                    .eq(ClassSubmission::getUserId, submission.getUserId())
                    .eq(ClassSubmission::getProblemId, submission.getProblemId())
                    .last("limit 1"));
            if (concurrent != null) {
                concurrent.setStatus(CLASS_SUBMISSION_AC);
                concurrent.setScore(SCORE_PER_PROBLEM);
                concurrent.setSubmissionId(submission.getId());
                if (concurrent.getSubmittedAt() == null) {
                    concurrent.setSubmittedAt(now);
                }
                classSubmissionMapper.updateById(concurrent);
            }
        }
    }

    private void updateProblemStats(OjProblem problem, JudgeResult result) {
        int submitCount = problem.getSubmitCount() == null ? 0 : problem.getSubmitCount();
        int acceptCount = problem.getAcceptCount() == null ? 0 : problem.getAcceptCount();
        submitCount++;
        if (result.getStatus() != null && result.getStatus().equals(JudgeStatusEnum.ACCEPTED.getCode())) {
            acceptCount++;
        }
        problem.setSubmitCount(submitCount);
        problem.setAcceptCount(acceptCount);
        problem.setAcceptRate(submitCount == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(acceptCount * 100.0 / submitCount).setScale(2, RoundingMode.HALF_UP));
        problem.setUpdatedAt(LocalDateTime.now());
        ojProblemMapper.updateById(problem);
    }

    private OjSubmissionVO submissionVO(OjSubmission submission) {
        return OjSubmissionVO.builder()
                .id(submission.getId())
                .problemId(submission.getProblemId())
                .userId(submission.getUserId())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .executionTime(submission.getExecutionTime())
                .memoryUsed(submission.getMemoryUsed())
                .passRate(submission.getPassRate())
                .passedCases(submission.getPassedCases())
                .totalCases(submission.getTotalCases())
                .errorMessage(submission.getErrorMessage())
                .judgeTime(submission.getJudgeTime())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}
