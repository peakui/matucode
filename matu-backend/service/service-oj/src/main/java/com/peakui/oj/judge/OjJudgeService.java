package com.peakui.oj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.peakui.oj.exception.OjException;
import com.peakui.oj.judge.sandbox.CodeSandbox;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeRequest;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeResponse;
import com.peakui.oj.mapper.OjProblemMapper;
import com.peakui.oj.mapper.OjSubmissionMapper;
import com.peakui.oj.mapper.OjTestCaseMapper;
import com.peakui.oj.model.entity.OjProblem;
import com.peakui.oj.model.entity.OjSubmission;
import com.peakui.oj.model.entity.OjTestCase;
import com.peakui.oj.model.vo.OjSubmissionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class OjJudgeService {

    private final OjSubmissionMapper ojSubmissionMapper;
    private final OjProblemMapper ojProblemMapper;
    private final OjTestCaseMapper ojTestCaseMapper;
    private final CodeSandbox codeSandbox;
    private final JudgeManager judgeManager;
    private final OjJudgeResultService ojJudgeResultService;

    public OjJudgeService(OjSubmissionMapper ojSubmissionMapper, OjProblemMapper ojProblemMapper,
                          OjTestCaseMapper ojTestCaseMapper, CodeSandbox codeSandbox, JudgeManager judgeManager,
                          OjJudgeResultService ojJudgeResultService) {
        this.ojSubmissionMapper = ojSubmissionMapper;
        this.ojProblemMapper = ojProblemMapper;
        this.ojTestCaseMapper = ojTestCaseMapper;
        this.codeSandbox = codeSandbox;
        this.judgeManager = judgeManager;
        this.ojJudgeResultService = ojJudgeResultService;
    }

    public OjSubmissionVO doJudge(Long submissionId) {
        OjSubmission submission = ojSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new OjException("提交记录不存在");
        }
        if (submission.getStatus() != null && !submission.getStatus().equals(JudgeStatusEnum.WAITING.getCode())) {
            return submissionVO(submission);
        }
        OjProblem problem = ojProblemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            throw new OjException("题目不存在");
        }
        List<OjTestCase> testCases = ojTestCaseMapper.selectList(new LambdaQueryWrapper<OjTestCase>()
                .eq(OjTestCase::getProblemId, submission.getProblemId())
                .orderByAsc(OjTestCase::getCaseNo));
        if (testCases.isEmpty()) {
            throw new OjException("测试用例不存在");
        }
        ExecuteCodeResponse response = codeSandbox.executeCode(ExecuteCodeRequest.builder()
                .language(submission.getLanguage())
                .code(submission.getCode())
                .inputList(testCases.stream().map(OjTestCase::getInput).toList())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .build());
        Integer sandboxStatus = response == null ? null : response.getStatus();
        boolean sandboxFailed = response == null || sandboxStatus == null
                || sandboxStatus == JudgeStatusEnum.SYSTEM_ERROR.getCode();
        boolean compileFailed = !sandboxFailed && sandboxStatus == JudgeStatusEnum.COMPILE_ERROR.getCode();
        String sandboxError = sandboxFailed
                ? (response == null ? "判题服务无响应" : firstNonBlank(response.getErrorMessage(), "判题服务异常"))
                : null;
        String compileMessage = compileFailed
                ? firstNonBlank(response.getCompileMessage(), response.getErrorMessage(), "编译错误")
                : null;
        JudgeContext context = JudgeContext.builder()
                .problem(problem)
                .submission(submission)
                .judgeCases(testCases.stream().map(item -> JudgeCaseDTO.builder().caseNo(item.getCaseNo()).input(item.getInput()).expectedOutput(item.getExpectedOutput()).build()).toList())
                .outputList(response == null || response.getOutputList() == null ? Collections.emptyList() : response.getOutputList())
                .executeCaseResults(response == null || response.getCaseResults() == null ? Collections.emptyList() : response.getCaseResults())
                .executionTime(response == null || response.getExecutionTime() == null ? 0 : response.getExecutionTime())
                .memoryUsed(response == null || response.getMemoryUsed() == null ? 0 : response.getMemoryUsed())
                .sandboxStatus(sandboxStatus)
                .compileMessage(compileMessage)
                .sandboxError(sandboxError)
                .build();
        JudgeResult result = judgeManager.doJudge(submission.getLanguage(), context);
        return ojJudgeResultService.saveJudgeResult(submission, problem, result);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
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
