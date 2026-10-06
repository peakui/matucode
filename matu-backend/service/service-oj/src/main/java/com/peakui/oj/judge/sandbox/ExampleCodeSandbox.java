package com.peakui.oj.judge.sandbox;

import com.peakui.oj.judge.JudgeStatusEnum;
import com.peakui.oj.judge.sandbox.model.ExecuteCaseResult;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeRequest;
import com.peakui.oj.judge.sandbox.model.ExecuteCodeResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "oj.sandbox", name = "type", havingValue = "example", matchIfMissing = true)
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        List<String> outputs = new ArrayList<>();
        List<ExecuteCaseResult> caseResults = new ArrayList<>();
        if (request.getInputList() != null) {
            for (int i = 0; i < request.getInputList().size(); i++) {
                String input = request.getInputList().get(i);
                outputs.add(input);
                caseResults.add(ExecuteCaseResult.builder()
                        .caseNo(i + 1)
                        .status(JudgeStatusEnum.ACCEPTED.getCode())
                        .output(input)
                        .executionTime(1)
                        .memoryUsed(128)
                        .exitCode(0)
                        .errorMessage(null)
                        .stderr(null)
                        .build());
            }
        }
        return ExecuteCodeResponse.builder()
                .outputList(outputs)
                .caseResults(caseResults)
                .status(JudgeStatusEnum.ACCEPTED.getCode())
                .executionTime(1)
                .memoryUsed(128)
                .errorMessage(null)
                .compileMessage(null)
                .build();
    }
}
