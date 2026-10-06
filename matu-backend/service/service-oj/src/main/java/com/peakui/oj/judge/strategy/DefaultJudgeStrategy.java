package com.peakui.oj.judge.strategy;

import com.peakui.oj.judge.JudgeCaseDTO;
import com.peakui.oj.judge.JudgeCaseResult;
import com.peakui.oj.judge.JudgeContext;
import com.peakui.oj.judge.JudgeResult;
import com.peakui.oj.judge.JudgeStatusEnum;
import com.peakui.oj.judge.JudgeStrategy;
import com.peakui.oj.judge.sandbox.model.ExecuteCaseResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class DefaultJudgeStrategy implements JudgeStrategy {

    @Override
    public boolean supports(String language) {
        return true;
    }

    @Override
    public JudgeResult judge(JudgeContext context) {
        List<JudgeCaseResult> caseResults = new ArrayList<>();
        if (context.getSandboxStatus() != null
                && context.getSandboxStatus() == JudgeStatusEnum.COMPILE_ERROR.getCode()) {
            return compileErrorResult(context);
        }
        if (context.getSandboxError() != null) {
            for (JudgeCaseDTO judgeCase : context.getJudgeCases()) {
                caseResults.add(JudgeCaseResult.builder()
                        .caseNo(judgeCase.getCaseNo())
                        .input(judgeCase.getInput())
                        .expectedOutput(judgeCase.getExpectedOutput())
                        .actualOutput(null)
                        .status(JudgeStatusEnum.SYSTEM_ERROR.getCode())
                        .executionTime(context.getExecutionTime())
                        .memoryUsed(context.getMemoryUsed())
                        .errorMessage(context.getSandboxError())
                        .build());
            }
            return JudgeResult.builder()
                    .status(JudgeStatusEnum.SYSTEM_ERROR.getCode())
                    .executionTime(context.getExecutionTime())
                    .memoryUsed(context.getMemoryUsed())
                    .passedCases(0)
                    .totalCases(context.getJudgeCases().size())
                    .passRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .errorMessage(context.getSandboxError())
                    .caseResults(caseResults)
                    .build();
        }
        int passed = 0;
        List<ExecuteCaseResult> executeResults = context.getExecuteCaseResults() == null ? List.of() : context.getExecuteCaseResults();
        int finalStatus = JudgeStatusEnum.ACCEPTED.getCode();
        for (int i = 0; i < context.getJudgeCases().size(); i++) {
            JudgeCaseDTO judgeCase = context.getJudgeCases().get(i);
            ExecuteCaseResult executeCaseResult = executeResults.size() > i ? executeResults.get(i) : null;
            String actual = executeCaseResult == null ? null : trim(executeCaseResult.getOutput());
            String expected = trim(judgeCase.getExpectedOutput());
            int caseStatus = resolveCaseStatus(executeCaseResult, actual, expected);
            if (caseStatus == JudgeStatusEnum.ACCEPTED.getCode()) {
                passed++;
            } else if (finalStatus == JudgeStatusEnum.ACCEPTED.getCode()) {
                finalStatus = caseStatus;
            }
            caseResults.add(JudgeCaseResult.builder()
                    .caseNo(judgeCase.getCaseNo())
                    .input(judgeCase.getInput())
                    .expectedOutput(judgeCase.getExpectedOutput())
                    .actualOutput(actual)
                    .status(caseStatus)
                    .executionTime(executeCaseResult == null ? context.getExecutionTime() : executeCaseResult.getExecutionTime())
                    .memoryUsed(executeCaseResult == null ? context.getMemoryUsed() : executeCaseResult.getMemoryUsed())
                    .errorMessage(executeCaseResult == null ? null : preferredError(executeCaseResult))
                    .build());
        }
        int total = context.getJudgeCases().size();
        BigDecimal passRate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(passed * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        return JudgeResult.builder()
                .status(passed == total ? JudgeStatusEnum.ACCEPTED.getCode() : finalStatus)
                .executionTime(context.getExecutionTime())
                .memoryUsed(context.getMemoryUsed())
                .passedCases(passed)
                .totalCases(total)
                .passRate(passRate)
                .errorMessage(passed == total ? null : statusMessage(finalStatus))
                .caseResults(caseResults)
                .build();
    }

    private JudgeResult compileErrorResult(JudgeContext context) {
        String message = context.getCompileMessage() != null
                ? context.getCompileMessage()
                : JudgeStatusEnum.COMPILE_ERROR.getMessage();
        List<JudgeCaseResult> caseResults = new ArrayList<>();
        for (JudgeCaseDTO judgeCase : context.getJudgeCases()) {
            caseResults.add(JudgeCaseResult.builder()
                    .caseNo(judgeCase.getCaseNo())
                    .input(judgeCase.getInput())
                    .expectedOutput(judgeCase.getExpectedOutput())
                    .actualOutput(null)
                    .status(JudgeStatusEnum.COMPILE_ERROR.getCode())
                    .executionTime(context.getExecutionTime())
                    .memoryUsed(context.getMemoryUsed())
                    .errorMessage(message)
                    .build());
        }
        return JudgeResult.builder()
                .status(JudgeStatusEnum.COMPILE_ERROR.getCode())
                .executionTime(context.getExecutionTime())
                .memoryUsed(context.getMemoryUsed())
                .passedCases(0)
                .totalCases(context.getJudgeCases().size())
                .passRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .errorMessage(message)
                .caseResults(caseResults)
                .build();
    }

    private int resolveCaseStatus(ExecuteCaseResult executeCaseResult, String actual, String expected) {
        if (executeCaseResult == null) {
            return JudgeStatusEnum.SYSTEM_ERROR.getCode();
        }
        if (executeCaseResult.getStatus() != null && !Objects.equals(executeCaseResult.getStatus(), JudgeStatusEnum.ACCEPTED.getCode())) {
            return executeCaseResult.getStatus();
        }
        if (Objects.equals(actual, expected)) {
            return JudgeStatusEnum.ACCEPTED.getCode();
        }
        return JudgeStatusEnum.WRONG_ANSWER.getCode();
    }

    private String preferredError(ExecuteCaseResult executeCaseResult) {
        if (executeCaseResult.getErrorMessage() != null) {
            return executeCaseResult.getErrorMessage();
        }
        return executeCaseResult.getStderr();
    }

    private String statusMessage(int status) {
        for (JudgeStatusEnum item : JudgeStatusEnum.values()) {
            if (item.getCode() == status) {
                return item.getMessage();
            }
        }
        return JudgeStatusEnum.SYSTEM_ERROR.getMessage();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
